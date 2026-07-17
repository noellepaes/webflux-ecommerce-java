package com.ecommerce.config;

import com.ecommerce.shared.domain.BaseEntity;
import org.reactivestreams.Publisher;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class R2dbcEntityCallbacks implements BeforeConvertCallback<BaseEntity>,
        AfterConvertCallback<BaseEntity>, AfterSaveCallback<BaseEntity> {

    @Override
    public Publisher<BaseEntity> onBeforeConvert(BaseEntity entity, SqlIdentifier table) {
        LocalDateTime now = LocalDateTime.now();
        if (entity.isNew() && entity.getId() == null) {
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        return Mono.just(entity);
    }

    @Override
    public Publisher<BaseEntity> onAfterConvert(BaseEntity entity, SqlIdentifier table) {
        entity.markNotNew();
        return Mono.just(entity);
    }

    @Override
    public Publisher<BaseEntity> onAfterSave(BaseEntity entity, OutboundRow outboundRow, SqlIdentifier table) {
        entity.markNotNew();
        return Mono.just(entity);
    }
}
