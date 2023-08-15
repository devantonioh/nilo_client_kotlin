package com.example.niloclient.chat

import com.example.niloclient.entities.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}