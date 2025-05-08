package com.capstone.rentit.locker.domain;

import java.io.Serializable;

public record DeviceLockerId(Long deviceId, Long lockerId) implements Serializable {}

