package com.odhiambopaul.springamazon.repositories;

import com.odhiambopaul.springamazon.domain.Image;
import org.springframework.data.repository.CrudRepository;

public interface ImageRepository extends CrudRepository<Image, Long> {
    Image findByImageFileName(String imageFileName);

    Image deleteByImageFileName(String imageFileName);
}
