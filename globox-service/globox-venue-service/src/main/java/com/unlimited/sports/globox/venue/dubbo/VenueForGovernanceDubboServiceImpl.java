package com.unlimited.sports.globox.venue.dubbo;

import com.unlimited.sports.globox.dubbo.governance.dto.ContentSnapshotResultDto;
import com.unlimited.sports.globox.dubbo.venue.VenueForGovernanceDubboService;
import com.unlimited.sports.globox.model.venue.entity.venues.VenueReview;
import com.unlimited.sports.globox.venue.mapper.venues.VenueReviewMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 场馆服务为治理服务提供的 dubbo 接口
 */
@Component
@DubboService(group = "rpc")
public class VenueForGovernanceDubboServiceImpl implements VenueForGovernanceDubboService {

    @Autowired
    private VenueReviewMapper venueReviewMapper;

    @Override
    public ContentSnapshotResultDto getVenueCommentSnapshot(Long id) {
        VenueReview venueReview = venueReviewMapper.selectById(id);

        ContentSnapshotResultDto resultDto = ContentSnapshotResultDto.builder()
                .id(id)
                .content(venueReview.getContent())
                .build();
        if (!ObjectUtils.isEmpty(venueReview.getImageUrls())) {
            String[] split = venueReview.getImageUrls().split(";");
            resultDto.setMediaList(List.of(split));
        }

        return resultDto;
    }
}
