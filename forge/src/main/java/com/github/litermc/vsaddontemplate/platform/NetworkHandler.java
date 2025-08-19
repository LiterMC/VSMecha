// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsaddontemplate.platform;

import com.github.litermc.vsaddontemplate.Constants;
import com.github.litermc.vsaddontemplate.network.MessageType;
import com.github.litermc.vsaddontemplate.network.NetworkMessage;
import com.github.litermc.vsaddontemplate.network.NetworkMessages;
import com.github.litermc.vsaddontemplate.network.client.ClientNetworkContext;
import com.github.litermc.vsaddontemplate.network.server.ServerNetworkContext;
import com.github.litermc.vsaddontemplate.platform.Services;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class NetworkHandler {
	private static final Logger LOG = LoggerFactory.getLogger(NetworkHandler.class);

	private static final SimpleChannel network;

	static {
		var version = Constants.MOD_VERSION;
		network = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(Constants.MOD_ID, "network"))
			.networkProtocolVersion(() -> version)
			.clientAcceptedVersions(version::equals).serverAcceptedVersions(version::equals)
			.simpleChannel();
	}

	private NetworkHandler() {
	}

	public static void setup() {
		for (var type : NetworkMessages.getServerbound()) {
			var forgeType = (MessageTypeImpl<? extends NetworkMessage<ServerNetworkContext>>) type;
			registerMainThread(forgeType, NetworkDirection.PLAY_TO_SERVER, c -> () -> c.getSender());
		}

		for (var type : NetworkMessages.getClientbound()) {
			var forgeType = (MessageTypeImpl<? extends NetworkMessage<ClientNetworkContext>>) type;
			registerMainThread(forgeType, NetworkDirection.PLAY_TO_CLIENT, x -> ClientHolder.get());
		}
	}

	@SuppressWarnings("unchecked")
	public static Packet<ClientGamePacketListener> createClientboundPacket(NetworkMessage<ClientNetworkContext> packet) {
		return (Packet<ClientGamePacketListener>) network.toVanillaPacket(packet, NetworkDirection.PLAY_TO_CLIENT);
	}

	@SuppressWarnings("unchecked")
	public static Packet<ServerGamePacketListener> createServerboundPacket(NetworkMessage<ServerNetworkContext> packet) {
		return (Packet<ServerGamePacketListener>) network.toVanillaPacket(packet, NetworkDirection.PLAY_TO_SERVER);
	}

	/**
	 * Register packet, and a thread-unsafe handler for it.
	 *
	 * @param <T>       The type of the packet to send.
	 * @param <H>       The context this packet is evaluated under.
	 * @param type      The message type to register.
	 * @param direction A network direction which will be asserted before any processing of this message occurs
	 * @param handler   Gets or constructs the handler for this packet.
	 */
	static <H, T extends NetworkMessage<H>> void registerMainThread(
		MessageTypeImpl<T> type, NetworkDirection direction, Function<NetworkEvent.Context, H> handler
	) {
		network.messageBuilder(type.klass(), type.id(), direction)
			.encoder(NetworkMessage::write)
			.decoder(type.reader())
			.consumerMainThread((packet, contextSup) -> {
				try {
					packet.handle(handler.apply(contextSup.get()));
				} catch (RuntimeException | Error e) {
					LOG.error("Failed handling packet", e);
					throw e;
				}
			})
			.add();
	}

	public record MessageTypeImpl<T extends NetworkMessage<?>>(
		int id, Class<T> klass, Function<FriendlyByteBuf, T> reader
	) implements MessageType<T> {
	}

	/**
	 * This holds an instance of {@link ClientNetworkContext}. This is a separate class to ensure that the instance is
	 * lazily created when needed on the client.
	 */
	private static final class ClientHolder {
		private static final @Nullable ClientNetworkContext INSTANCE;
		private static final @Nullable Throwable ERROR;

		static {
			var helper = Services.tryLoad(ClientNetworkContext.class);
			INSTANCE = helper.instance();
			ERROR = helper.error();
		}

		static ClientNetworkContext get() {
			var instance = INSTANCE;
			return instance == null ? Services.raise(ClientNetworkContext.class, ERROR) : instance;
		}
	}
}
