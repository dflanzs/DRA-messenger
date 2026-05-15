package com.example.mobile_app.data.network

import com.example.mobile_app.data.model.chat.ChatUserDto
import com.example.mobile_app.data.model.chat.CreateDirectChatRequestDto
import com.example.mobile_app.data.model.chat.CreateGroupChatRequestDto
import com.example.mobile_app.data.model.chat.DirectChatSummaryDto
import com.example.mobile_app.data.model.chat.GroupChatSummaryDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatApiService {
    @GET("api/users/active")
    suspend fun listActiveUsers(): List<ChatUserDto>

    @GET("api/private-chats/me")
    suspend fun listPrivateChats(): List<DirectChatSummaryDto>

    @GET("api/group-chats/me")
    suspend fun listGroupChats(): List<GroupChatSummaryDto>

    @POST("api/private-chats")
    suspend fun createDirectChat(
        @Body request: CreateDirectChatRequestDto,
    ): DirectChatSummaryDto

    @POST("api/group-chats")
    suspend fun createGroupChat(
        @Body request: CreateGroupChatRequestDto,
    ): GroupChatSummaryDto

    @GET("api/group-chats/{groupId}/users")
    suspend fun getGroupMembers(
        @Path("groupId") groupId: Long,
    ): Set<Long>
}
