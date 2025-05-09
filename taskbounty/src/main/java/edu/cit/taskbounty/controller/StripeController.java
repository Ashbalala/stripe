package edu.cit.taskbounty.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.AccountLink;
import com.stripe.model.Payout;
import com.stripe.model.Transfer;
import com.stripe.model.checkout.Session;
import edu.cit.taskbounty.model.BountyPost;
import edu.cit.taskbounty.model.Solution;
import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.BountyPostRepository;
import edu.cit.taskbounty.repository.SolutionRepository;
import edu.cit.taskbounty.repository.UserRepository;
import edu.cit.taskbounty.service.BountyPostService;
import edu.cit.taskbounty.service.ProcessedDonationService;
import edu.cit.taskbounty.service.SolutionService;
import edu.cit.taskbounty.service.StripeService;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/stripe")
public class StripeController {

    private static final Logger logger = LoggerFactory.getLogger(StripeController.class);

    @Value("${url}")
    private String url;

    @Autowired
    private StripeService stripeService;

    @Autowired
    private BountyPostService bountyPostService;

    @Autowired
    private BountyPostRepository bountyPostRepository;

    @Autowired
    private ProcessedDonationService processedDonationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SolutionService solutionService;
    @Autowired
    private SolutionRepository solutionRepository;

    @GetMapping("/checkout/{bountyPostId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createCheckoutSession(@PathVariable String bountyPostId) {
        logger.info("Creating checkout session for bountyPostId: {}", bountyPostId);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            logger.debug("Fetching current user: {}", username);
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                logger.warn("User {} not found", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User not found");
            }

            logger.debug("Fetching bounty post: {}", bountyPostId);
            ResponseEntity<BountyPost> bountyPostResponse = bountyPostService.getBountyPostById(new ObjectId(bountyPostId));
            if (bountyPostResponse.getStatusCode() != HttpStatus.OK || bountyPostResponse.getBody() == null) {
                logger.warn("Bounty post {} not found or inaccessible", bountyPostId);
                return ResponseEntity.status(bountyPostResponse.getStatusCode())
                        .body("Bounty post not found or you don't have permission to access it");
            }
            BountyPost bountyPost = bountyPostResponse.getBody();

            if (!bountyPost.getCreatorId().equals(currentUser.getId())) {
                logger.warn("User {} is not the creator of bounty post {}", currentUser.getId(), bountyPostId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only the bounty creator can create a checkout session");
            }

            if (bountyPost.isPublic()) {
                logger.warn("Bounty post {} is already public", bountyPostId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Bounty post is already public");
            }

            long amount = bountyPost.getBountyPrice().multiply(new BigDecimal("100")).longValue();
            String successUrl = url + "/stripe/payment_success/bounty_post?bountyPostId=" + bountyPostId + "&session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = url + "/payment/cancel";
            String itemName = "Bounty Post: " + bountyPost.getTitle();
            logger.debug("Creating checkout session: amount: {} PHP, item: {}", amount, itemName);
            String sessionUrl = stripeService.createCheckoutSession(bountyPostId, amount, "php", successUrl, cancelUrl, itemName);
            logger.info("Checkout session created, URL: {}", sessionUrl);

            return ResponseEntity.ok(sessionUrl);
        } catch (StripeException e) {
            logger.error("Error creating checkout session for bountyPostId: {}. Error: {}", bountyPostId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating checkout session: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid bounty post ID: {}. Error: {}", bountyPostId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid bounty post ID");
        }
    }

    @GetMapping("/payment_success/bounty_post")
    public ResponseEntity<String> confirmPayment(
            @RequestParam String bountyPostId,
            @RequestParam String session_id) {
        logger.info("Confirming payment for bountyPostId: {}", bountyPostId);
        try {
            if (processedDonationService.isProcessed(session_id)) {
                logger.warn("Payment already processed for session_id: {}", session_id);
                return ResponseEntity.badRequest().body("Payment has already been processed");
            }

            logger.debug("Retrieving Stripe session: {}", session_id);
            Session session = Session.retrieve(session_id);
            if ("paid".equals(session.getPaymentStatus()) && bountyPostId.equals(session.getMetadata().get("bountyPostId"))) {
                logger.debug("Fetching bounty post: {}", bountyPostId);
                ResponseEntity<BountyPost> bountyPostResponse = bountyPostService.getBountyPostById(new ObjectId(bountyPostId));
                if (bountyPostResponse.getStatusCode() != HttpStatus.OK || bountyPostResponse.getBody() == null) {
                    logger.warn("Bounty post {} not found or inaccessible", bountyPostId);
                    return ResponseEntity.status(bountyPostResponse.getStatusCode())
                            .body("Bounty post not found or inaccessible");
                }
                BountyPost post = bountyPostResponse.getBody();
                post.setPublic(true);
                bountyPostRepository.save(post);
                logger.info("Bounty post {} set to public", bountyPostId);

                processedDonationService.markAsProcessed(session_id);
                return ResponseEntity.ok("Payment successful, post is now public");
            }
            logger.warn("Payment failed or session invalid for session_id: {}", session_id);
            return ResponseEntity.badRequest().body("Payment failed or session invalid");
        } catch (StripeException e) {
            logger.error("Error confirming payment for bountyPostId: {}. Error: {}", bountyPostId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error confirming payment: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Invalid bounty post ID: {}. Error: {}", bountyPostId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid bounty post ID");
        }
    }

    @PostMapping("/approve_solution/payout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> approveSolutionAndPayout(@RequestParam String solutionId) {
        logger.info("Approving solution and initiating payout for solutionId: {}", solutionId);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            logger.debug("Fetching current user: {}", username);
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                logger.warn("User {} not found", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User not found");
            }

            logger.debug("Fetching solution: {}", solutionId);
            Optional<Solution> solutionOpt = solutionService.getSolutionByIdAndBountyPostId(solutionId, null);
            if (solutionOpt.isEmpty()) {
                logger.warn("Solution {} not found", solutionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Solution not found");
            }
            Solution solution = solutionOpt.get();
            String bountyPostId = solution.getBountyPostId();

            logger.debug("Fetching bounty post: {}", bountyPostId);
            ResponseEntity<BountyPost> bountyPostResponse = bountyPostService.getBountyPostById(new ObjectId(bountyPostId));
            if (bountyPostResponse.getStatusCode() != HttpStatus.OK || bountyPostResponse.getBody() == null) {
                logger.warn("Bounty post {} not found or inaccessible", bountyPostId);
                return ResponseEntity.status(bountyPostResponse.getStatusCode())
                        .body("Bounty post not found or inaccessible");
            }
            BountyPost bountyPost = bountyPostResponse.getBody();

            if (!bountyPost.getCreatorId().equals(currentUser.getId())) {
                logger.warn("User {} is not the creator of bounty post {}", currentUser.getId(), bountyPostId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only the bounty creator can approve a solution");
            }

            if (solution.isApproved()) {
                logger.warn("Solution {} is already approved for bountyPostId: {}", solutionId, bountyPostId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Solution is already approved");
            }

            Solution approvedSolution = solutionService.getSolutionsByBountyPostId(bountyPostId, 0, 1)
                    .getContent()
                    .stream()
                    .filter(Solution::isApproved)
                    .findFirst()
                    .orElse(null);
            if (approvedSolution != null) {
                logger.warn("Another solution already approved for bountyPostId: {}", bountyPostId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Another solution is already approved for this bounty post");
            }

            logger.debug("Fetching submitter: {}", solution.getSubmitterId());
            User submitter = userRepository.findById(solution.getSubmitterId())
                    .orElseThrow(() -> {
                        logger.error("Submitter {} not found for solution {}", solution.getSubmitterId(), solutionId);
                        return new RuntimeException("Submitter not found");
                    });

            if (submitter.getStripeAccountId() == null) {
                logger.warn("Submitter {} has no Stripe account for solution {}", submitter.getId(), solutionId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Submitter does not have a Stripe account");
            }

            long amount = bountyPost.getBountyPrice().multiply(new BigDecimal("100")).longValue();
            String currency = "php";

            logger.debug("Retrieving Stripe account: {}", submitter.getStripeAccountId());
            Account stripeAccount = Account.retrieve(submitter.getStripeAccountId());
            String externalAccountId = stripeAccount.getExternalAccounts()
                    .getData()
                    .stream()
                    .findFirst()
                    .map(externalAccount -> externalAccount.getId())
                    .orElse(null);
            if (externalAccountId == null) {
                logger.warn("No external account for Stripe account: {}", submitter.getStripeAccountId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Submitter has no linked external account for payout");
            }

            logger.debug("Initiating payout: amount {} {}", amount, currency);
            Payout payout = stripeService.createPayout(submitter.getStripeAccountId(), amount, currency, externalAccountId);
            logger.info("Payout created with ID: {} for bountyPostId: {}", payout.getId(), bountyPostId);

            solution.setApproved(true);
            solutionRepository.save(solution);
            logger.info("Solution {} approved for bountyPostId: {}", solutionId, bountyPostId);

            return ResponseEntity.ok("Solution approved and payout initiated successfully");
        } catch (StripeException e) {
            logger.error("Error processing payout for solutionId: {}. Error: {}", solutionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payout: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid bounty post ID");
        } catch (RuntimeException e) {
            logger.error("Error processing payout for solutionId: {}. Error: {}", solutionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing payout: " + e.getMessage());
        }
    }

    @PostMapping("/approve_solution/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> approveSolutionAndTransfer(@RequestParam String solutionId) {
        logger.info("Approving solution and initiating transfer for solutionId: {}", solutionId);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            logger.debug("Fetching current user: {}", username);
            User currentUser = userRepository.findByUsername(username);
            if (currentUser == null) {
                logger.warn("User {} not found", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User not found");
            }

            logger.debug("Fetching solution: {}", solutionId);
            Optional<Solution> solutionOpt = solutionRepository.findById(solutionId);
            if (solutionOpt.isEmpty()) {
                logger.warn("Solution {} not found", solutionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Solution not found");
            }
            Solution solution = solutionOpt.get();
            String bountyPostId = solution.getBountyPostId();

            logger.debug("Fetching bounty post: {}", bountyPostId);
            ResponseEntity<BountyPost> bountyPostResponse = bountyPostService.getBountyPostById(new ObjectId(bountyPostId));
            if (bountyPostResponse.getStatusCode() != HttpStatus.OK || bountyPostResponse.getBody() == null) {
                logger.warn("Bounty post {} not found or inaccessible", bountyPostId);
                return ResponseEntity.status(bountyPostResponse.getStatusCode())
                        .body("Bounty post not found or inaccessible");
            }
            BountyPost bountyPost = bountyPostResponse.getBody();

            if (!bountyPost.getCreatorId().equals(currentUser.getId())) {
                logger.warn("User {} is not the creator of bounty post {}", currentUser.getId(), bountyPostId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("Only the bounty creator can approve a solution");
            }

            if (solution.isApproved()) {
                logger.warn("Solution {} is already approved for bountyPostId: {}", solutionId, bountyPostId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Solution is already approved");
            }

            Solution approvedSolution = solutionService.getSolutionsByBountyPostId(bountyPostId, 0, 1)
                    .getContent()
                    .stream()
                    .filter(Solution::isApproved)
                    .findFirst()
                    .orElse(null);
            if (approvedSolution != null) {
                logger.warn("Another solution already approved for bountyPostId: {}", bountyPostId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Another solution is already approved for this bounty post");
            }

            logger.debug("Fetching submitter: {}", solution.getSubmitterId());
            User submitter = userRepository.findById(solution.getSubmitterId())
                    .orElseThrow(() -> {
                        logger.error("Submitter {} not found for solution {}", solution.getSubmitterId(), solutionId);
                        return new RuntimeException("Submitter not found");
                    });

            if (submitter.getStripeAccountId() == null) {
                logger.warn("Submitter {} has no Stripe account for solution {}", submitter.getId(), solutionId);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Submitter does not have a Stripe account");
            }

            long amount = bountyPost.getBountyPrice().multiply(new BigDecimal("100")).longValue();
            String currency = "php";

            logger.debug("Initiating transfer: amount {} {}", amount, currency);
            Transfer transfer = (Transfer) stripeService.createTransfer(submitter.getStripeAccountId(), amount, currency).getBody();
            assert transfer != null;
            logger.info("Transfer created with ID: {} for bountyPostId: {}", transfer.getId(), bountyPostId);

            solution.setApproved(true);
            solutionRepository.save(solution);
            logger.info("Solution {} approved for bountyPostId: {}", solutionId, bountyPostId);

            return ResponseEntity.ok("Solution approved and transfer initiated successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid bounty post ID");
        } catch (StripeException | RuntimeException e) {
            logger.error("Error processing transfer for solutionId: {}. Error: {}", solutionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing transfer: " + e.getMessage());
        }
    }

    @GetMapping("/onboarding")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> onboardUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        try {
            User user = userRepository.findByUsername(username);
            logger.debug("Fetching user: {}", user.getId());
            if (user.getStripeAccountId() == null) {
                logger.warn("User {} does not have a Stripe account", user.getId());
                return ResponseEntity.badRequest().body("User does not have a Stripe account");
            }

            String refreshUrl = url + "/refresh-onboarding";
            String returnUrl = url + "/onboarding-complete";
            logger.debug("Creating account link for Stripe account: {}", user.getStripeAccountId());
            AccountLink accountLink = stripeService.createAccountLinkForOnboarding(user.getStripeAccountId(), refreshUrl, returnUrl);
            logger.info("Onboarding link created: {}", accountLink.getUrl());

            return ResponseEntity.ok(accountLink.getUrl());
        } catch (StripeException e) {
            logger.error("Error creating onboarding link for userId: Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating onboarding link: " + e.getMessage());
        } catch (RuntimeException e) {
            logger.error("Error onboarding userId: Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/create_account")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> createAccount(@RequestParam String email) {
        logger.info("Creating Stripe account for email: {}", email);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            logger.debug("Fetching current user: {}", username);
            User user = userRepository.findByUsername(username);
            if (user == null) {
                logger.warn("User {} not found", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("User not found");
            }

            logger.debug("Creating Express account with email: {}", email);
            Account account = stripeService.createExpressAccount(email);
            user.setStripeAccountId(account.getId());
            userRepository.save(user);
            logger.info("Stripe account created with ID: {}", account.getId());

            return ResponseEntity.ok("Account created with ID: " + account.getId());
        } catch (StripeException e) {
            logger.error("Error creating Stripe account for email: {}. Error: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating account: " + e.getMessage());
        }
    }
}