package edu.cit.taskbounty.util;

import com.stripe.Stripe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class StripeConfig {

    private static final Logger logger = LoggerFactory.getLogger(StripeConfig.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @PostConstruct
    public void init() {
        logger.info("Initializing Stripe with API key ending in: {}", stripeApiKey.substring(Math.max(stripeApiKey.length() - 4, 0)));
        Stripe.apiKey = stripeApiKey;
    }
}