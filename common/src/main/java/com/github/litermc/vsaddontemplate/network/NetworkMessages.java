// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsaddontemplate.network;

import com.github.litermc.vsaddontemplate.Constants;
import com.github.litermc.vsaddontemplate.network.client.*;
import com.github.litermc.vsaddontemplate.network.server.*;
import com.github.litermc.vsaddontemplate.platform.PlatformHelper;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * List of all {@link MessageType}s provided by CC: Tweaked.
 *
 * @see PlatformHelper The platform helper is used to send packets.
 */
public final class NetworkMessages {
	private static final IntSet seenIds = new IntOpenHashSet();
	private static final Set<String> seenChannel = new HashSet<>();
	private static final List<MessageType<? extends NetworkMessage<ServerNetworkContext>>> serverMessages = new ArrayList<>();
	private static final List<MessageType<? extends NetworkMessage<ClientNetworkContext>>> clientMessages = new ArrayList<>();

	// public static final MessageType<ComputerActionServerMessage> COMPUTER_ACTION = registerServerbound(0, "computer_action", ComputerActionServerMessage.class, ComputerActionServerMessage::new);

	// public static final MessageType<ChatTableClientMessage> CHAT_TABLE = registerClientbound(10, "chat_table", ChatTableClientMessage.class, ChatTableClientMessage::new);

	private NetworkMessages() {
	}

	private static <C, T extends NetworkMessage<C>> MessageType<T> register(
		List<MessageType<? extends NetworkMessage<C>>> messages,
		int id, String channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader
	) {
		if (!seenIds.add(id)) throw new IllegalArgumentException("Duplicate id " + id);
		if (!seenChannel.add(channel)) throw new IllegalArgumentException("Duplicate channel " + channel);
		var type = PlatformHelper.get().createMessageType(id, new ResourceLocation(Constants.MOD_ID, channel), klass, reader);
		messages.add(type);
		return type;
	}

	private static <T extends NetworkMessage<ServerNetworkContext>> MessageType<T> registerServerbound(int id, String channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
		return register(serverMessages, id, channel, klass, reader);
	}

	private static <T extends NetworkMessage<ClientNetworkContext>> MessageType<T> registerClientbound(int id, String channel, Class<T> klass, FriendlyByteBuf.Reader<T> reader) {
		return register(clientMessages, id, channel, klass, reader);
	}

	/**
	 * Get all serverbound message types.
	 *
	 * @return An unmodifiable sequence of all serverbound message types.
	 */
	public static Collection<MessageType<? extends NetworkMessage<ServerNetworkContext>>> getServerbound() {
		return Collections.unmodifiableCollection(serverMessages);
	}

	/**
	 * Get all clientbound message types.
	 *
	 * @return An unmodifiable sequence of all clientbound message types.
	 */
	public static Collection<MessageType<? extends NetworkMessage<ClientNetworkContext>>> getClientbound() {
		return Collections.unmodifiableCollection(clientMessages);
	}
}
