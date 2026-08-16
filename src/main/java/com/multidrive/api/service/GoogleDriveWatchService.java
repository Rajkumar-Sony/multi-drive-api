package com.multidrive.api.service;

import com.multidrive.api.entity.GoogleDriveWatchChannel;

import java.util.List;

public interface GoogleDriveWatchService {

	List<GoogleDriveWatchChannel> registerWatchChannels(Long connectionId, Long userId);

}