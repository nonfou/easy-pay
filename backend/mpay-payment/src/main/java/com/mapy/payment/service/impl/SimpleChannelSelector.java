package com.mapy.payment.service.impl;

import com.mapy.payment.repository.PayAccountRepository;
import com.mapy.payment.repository.PayChannelRepository;
import com.mapy.payment.service.ChannelSelector;
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
                            channel.setLastTime(LocalDateTime.now().toString());
                            return new ChannelSelection(account.getId(), channel.getId(), account.getPattern());
                        }));
    }
}
