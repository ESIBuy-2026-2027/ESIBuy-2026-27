package es.uclm.esibuy.reviews.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import es.uclm.esibuy.reviews.model.Review;

@ExtendWith(MockitoExtension.class)
class ReviewIndexInitializerTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private IndexOperations indexOperations;

    @Test
    void createsUniqueIndexForCustomerOrderAndProduct() {
        when(mongoTemplate.indexOps(Review.class)).thenReturn(indexOperations);
        ReviewIndexInitializer initializer = new ReviewIndexInitializer(mongoTemplate);

        initializer.run(new DefaultApplicationArguments());

        verify(indexOperations).ensureIndex(org.mockito.ArgumentMatchers.any(Index.class));
    }
}
