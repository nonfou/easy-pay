package com.github.nonfou.mpay.service.impl;

import com.github.nonfou.mpay.repository.PayAccountRepository;
import com.github.nonfou.mpay.repository.PayChannelRepository;
import com.github.nonfou.mpay.service.ChannelSelector;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SimpleChannelSelector implements ChannelSelector {

    private final PayAccountRepository payAccountRepository;
    private final PayChannelRepository payChannelRepository;

    public SimpleChannelSelector(PayAccountRepository payAccountRepository,
            PayChannelRepository payChannelRepository) {
        this.payAccountRepository = payAccountRepository;
        this.payChannelRepository = payChannelRepository;
    }

    @Override
    public Optional<ChannelSelection> select(Long pid, String payType) {
        return payAccountRepository.findByPidAndState(pid, 1).stream()
                .findFirst()
                .flatMap(account -> payChannelRepository.findAll().stream()
                        .filter(channel -> channel.getAccount().getId().equals(account.getId()))
                        .findFirst()
                        .map(channel -> {
                            channel.setLastTime(LocalDateTime.now());
                            return new ChannelSelection(account.getId(), channel.getId(), account.getPattern());
                        }));
    }
}
