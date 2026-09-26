package com.soundzone.common;

import com.soundzone.moment.repository.MomentRepository;
import com.soundzone.queue.repository.QueueItemRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 控制器先定位不可变所属域，再开启业务事务；避免 MySQL RR 旧快照和嵌套连接。 */
@Service
@RequiredArgsConstructor
public class ResourceLocator {
    private final QueueItemRepository queue;
    private final MomentRepository moments;

    @Transactional(readOnly = true)
    public Long queueZone(Long id) {
        return queue.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.QUEUE_ITEM_NOT_FOUND))
                .getZone()
                .getId();
    }

    @Transactional(readOnly = true)
    public Long momentZone(Long id) {
        return moments.findById(id)
                .orElseThrow(() -> new BizException(ResultCode.MOMENT_NOT_FOUND))
                .getZone()
                .getId();
    }
}
