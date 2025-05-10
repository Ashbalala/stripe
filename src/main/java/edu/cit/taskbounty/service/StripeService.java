package edu.cit.taskbounty.service;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StripeService {

    private static final Logger logger = LoggerFactory.getLogger(StripeService.class);

    public Account createExpressAccount(String email) throws StripeException {
        logger.debug("Creating Express account for email: {}", email);
        Map<String, Object> params = new HashMap<>();
        params.put("type", "express");
        params.put("country", "US");
        params.put("email", email);

        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("card_payments", new HashMap<>());
        capabilities.put("transfers", new HashMap<>());
        params.put("capabilities", capabilities);

        Account account = Account.create(params);
        logger.info("Express account created with ID: {}", account.getId());
        return account;
    }

    public ResponseEntity<Transfer> createTransfer(String connectedAccountId, long amount, String currency) throws StripeException {
        logger.debug("Creating transfer of {} {} to account: {}", amount, currency, connectedAccountId);

        Balance balance = Balance.retrieve();
        long availableBalance = balance.getAvailable()
                .stream()
                .filter(b -> currency.equalsIgnoreCase(b.getCurrency()))
                .mapToLong(Balance.Available::getAmount)
                .sum();

        if (availableBalance < amount && !balance.getLivemode()) {
            logger.warn("Insufficient test balance, creating test charge...");

            Map<String, Object> cardParams = new HashMap<>();
            cardParams.put("type", "card");
            cardParams.put("card[number]", "4000000000000077");
            cardParams.put("card[exp_month]", 12);
            cardParams.put("card[exp_year]", 2026);
            cardParams.put("card[cvc]", "123");

            PaymentMethod paymentMethod = PaymentMethod.create(cardParams);

            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", amount);
            chargeParams.put("currency", currency);
            chargeParams.put("payment_method", paymentMethod.getId());
            chargeParams.put("confirm", true);
            Charge.create(chargeParams);

            balance = Balance.retrieve();
            availableBalance = balance.getAvailable()
                    .stream()
                    .filter(b -> currency.equalsIgnoreCase(b.getCurrency()))
                    .mapToLong(Balance.Available::getAmount)
                    .sum();

            if (availableBalance < amount) {
                logger.error("Still insufficient after test charge");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Transfer());
            }
        }

        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency);
        params.put("destination", connectedAccountId);
        params.put("description", "Payment to customer");

        Transfer transfer = Transfer.create(params);
        logger.info("Transfer created with ID: {}", transfer.getId());
        return ResponseEntity.ok(transfer);
    }

    public Payout createPayout(String connectedAccountId, long amount, String currency, String externalAccountId) throws StripeException {
        logger.debug("Creating payout of {} {} to account: {}, external: {}", amount, currency, connectedAccountId, externalAccountId);
        Map<String, Object> params = new HashMap<>();
        params.put("amount", amount);
        params.put("currency", currency);
        params.put("destination", externalAccountId);
        params.put("description", "Payout to customer");

        Payout payout = Payout.create(params, new com.stripe.net.RequestOptions.RequestOptionsBuilder()
                .setStripeAccount(connectedAccountId)
                .build());
        logger.info("Payout created with ID: {}", payout.getId());
        return payout;
    }

    public AccountLink createAccountLinkForOnboarding(String connectedAccountId, String refreshUrl, String returnUrl) throws StripeException {
        logger.debug("Creating account link for onboarding");
        Map<String, Object> params = new HashMap<>();
        params.put("account", connectedAccountId);
        params.put("refresh_url", refreshUrl);
        params.put("return_url", returnUrl);
        params.put("type", "account_onboarding");

        AccountLink link = AccountLink.create(params);
        logger.info("Account link created: {}", link.getUrl());
        return link;
    }

    public String createCheckoutSession(String bountyPostId, long amount, String currency, String successUrl, String cancelUrl, String itemName) throws StripeException {
        logger.debug("Creating checkout session for bountyPostId: {}", bountyPostId);

        // Local testing redirect page (contains redirect to bountymobile://payment_success)
        String redirectSuccessUrl = "https://taskbounty-434679566601.us-west2.run.app/";

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(redirectSuccessUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setUnitAmount(amount)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(itemName)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .putMetadata("bountyPostId", bountyPostId)
                .build();

        Session session = Session.create(params);
        logger.info("Checkout session created, URL: {}", session.getUrl());
        return session.getUrl();
    }
}