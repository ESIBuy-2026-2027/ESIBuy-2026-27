package es.uclm.esibuy.reviews.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import es.uclm.esibuy.reviews.model.Review;

@Component
public class ReviewIndexInitializer implements ApplicationRunner {

    private static final String UNIQUE_REVIEW_INDEX = "customer_order_product_unique";

    private final MongoTemplate mongoTemplate;

    public ReviewIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        Index index = new Index()
                .on("customerId", Sort.Direction.ASC)
                .on("orderId", Sort.Direction.ASC)
                .on("productId", Sort.Direction.ASC)
                .unique()
                .named(UNIQUE_REVIEW_INDEX);

        mongoTemplate.indexOps(Review.class).ensureIndex(index);
    }
}
