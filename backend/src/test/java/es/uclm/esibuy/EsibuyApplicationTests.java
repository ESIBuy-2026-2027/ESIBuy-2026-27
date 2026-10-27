package es.uclm.esibuy;

import es.uclm.esibuy.categories.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
class EsibuyApplicationTests {

	@MockBean
	private CategoryRepository categoryRepository;

	@Test
	void contextLoads() {
		// Intentionally empty: this test only verifies that the Spring application
		// context starts successfully with all required beans and configuration.
	}

}