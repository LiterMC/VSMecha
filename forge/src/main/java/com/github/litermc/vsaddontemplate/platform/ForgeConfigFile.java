// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vsaddontemplate.platform;

import com.github.litermc.vsaddontemplate.config.ConfigFile;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A {@link ConfigFile} which wraps Forge's config implementation.
 */
public final class ForgeConfigFile extends ConfigFile {
	private final ForgeConfigSpec spec;

	private ForgeConfigFile(ForgeConfigSpec spec, Map<String, Entry> entries) {
		super(entries);
		this.spec = spec;
	}

	public ForgeConfigSpec spec() {
		return spec;
	}

	/**
	 * Wraps {@link ForgeConfigSpec.Builder} into our own config builder abstraction.
	 */
	static class Builder extends ConfigFile.Builder {
		private final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

		private void translation(String name) {
			builder.translation(getTranslation(name));
		}

		@Override
		public ConfigFile.Builder comment(String comment) {
			super.comment(comment);
			builder.comment(comment);
			return this;
		}

		@Override
		public void push(String name) {
			super.push(name);

			translation(name);
			builder.push(name);
		}

		@Override
		public void pop() {
			super.pop();
			builder.pop();
		}

		@Override
		public ConfigFile.Builder worldRestart() {
			builder.worldRestart();
			return this;
		}

		private <T> ConfigFile.Value<T> defineValue(String name, ForgeConfigSpec.ConfigValue<T> value) {
			var wrapped = new ValueImpl<>(getPath(name), takeComment(), value);
			groupStack.getLast().children().put(name, wrapped);
			return wrapped;
		}

		@Override
		public <T> ConfigFile.Value<T> define(String name, T defaultValue) {
			translation(name);
			return defineValue(name, builder.define(name, defaultValue));
		}

		@Override
		public ConfigFile.Value<Boolean> define(String name, boolean defaultValue) {
			translation(name);
			return defineValue(name, builder.define(name, defaultValue));
		}

		@Override
		public ConfigFile.Value<Integer> defineInRange(String name, int defaultValue, int min, int max) {
			translation(name);
			return defineValue(name, builder.defineInRange(name, defaultValue, min, max));
		}

		@Override
		public <T> ConfigFile.Value<List<? extends T>> defineList(String name, List<? extends T> defaultValue, Predicate<Object> elementValidator) {
			translation(name);
			return defineValue(name, builder.defineList(name, defaultValue, elementValidator));
		}

		@Override
		public <V extends Enum<V>> ConfigFile.Value<V> defineEnum(String name, V defaultValue) {
			translation(name);
			return defineValue(name, builder.defineEnum(name, defaultValue));
		}

		@Override
		public ConfigFile build(ConfigListener onChange) {
			var children = groupStack.removeLast().children();
			if (!groupStack.isEmpty()) throw new IllegalStateException("Mismatched config push/pop");

			var spec = builder.build();
			return new ForgeConfigFile(spec, children);
		}
	}

	private static final class ValueImpl<T> extends Value<T> {
		private final ForgeConfigSpec.ConfigValue<T> value;

		private ValueImpl(String path, String comment, ForgeConfigSpec.ConfigValue<T> value) {
			super(path, comment);
			this.value = value;
		}

		@Override
		public T get() {
			return value.get();
		}
	}
}
