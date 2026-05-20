package com.example.mobile_app.data.network

import com.example.mobile_app.data.model.chat.ChatUserDto
import com.example.mobile_app.data.model.chat.CommunicationRequestDto
import com.example.mobile_app.data.model.chat.CreateDirectChatRequestDto
import com.example.mobile_app.data.model.chat.CreateGroupChatRequestDto
import com.example.mobile_app.data.model.chat.DirectChatResultDto
import com.example.mobile_app.data.model.chat.DirectChatSummaryDto
import com.example.mobile_app.data.model.chat.GroupChatSummaryDto
import com.example.mobile_app.data.model.signal.SignalMessageWSDto
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
    ): DirectChatResultDto

    @POST("api/group-chats")
    suspend fun createGroupChat(
        @Body request: CreateGroupChatRequestDto,
    ): GroupChatSummaryDto

    @GET("api/group-chats/{groupId}/users")
    suspend fun getGroupMembers(
        @Path("groupId") groupId: Long,
    ): Set<Long>

    @GET("api/communication-requests/incoming")
    suspend fun listIncomingRequests(): List<CommunicationRequestDto>

    @GET("api/communication-requests/outgoing")
    suspend fun listOutgoingRequests(): List<CommunicationRequestDto>

    @POST("api/communication-requests/{id}/accept")
    suspend fun acceptCommunicationRequest(
        @Path("id") id: Long,
    ): DirectChatSummaryDto

    @POST("api/communication-requests/{id}/reject")
    suspend fun rejectCommunicationRequest(
        @Path("id") id: Long,
    )

    @POST("api/messages/{envelopeId}/ack-delivered")
    suspend fun ackMessageDelivered(
        @Path("envelopeId") envelopeId: Long,
    )

    @GET("api/messages/pending")
    suspend fun getPendingMessages(): List<SignalMessageWSDto>
}
