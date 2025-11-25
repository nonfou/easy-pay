package com.github.nonfou.mpay.service;

import java.util.Optional;

public interface ChannelSelector {

    Optional<ChannelSelection> select(Long pid, String payType);

    record ChannelSelection(Long aid, Long cid, Integer pattern) {}
}
