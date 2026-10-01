package es.uclm.esibuy;

import es.uclm.esibuy.categories.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class EsibuyApplicationTests {

	@MockitoBean
	private CategoryRepository categoryRepository;

	@Test
	void contextLoads() {
	}

}
