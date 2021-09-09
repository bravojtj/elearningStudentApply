package store;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="applies", path="applies")
public interface ApplyRepository extends PagingAndSortingRepository<Apply, Long>{


}
