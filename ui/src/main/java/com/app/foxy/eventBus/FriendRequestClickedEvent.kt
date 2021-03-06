package com.app.foxy.eventBus

import com.app.data.network.apiResponse.FriendsRequestsResponse

/**
 * Triggered when the user on accept or decline for a friend request
 */
class FriendRequestClickedEvent(var isAccepted: Boolean, var requestId: String, var notificationId: String,  var request: FriendsRequestsResponse)