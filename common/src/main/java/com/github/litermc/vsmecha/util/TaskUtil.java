package com.github.litermc.vsmecha.util;

import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public final class TaskUtil {
	private static final Queue<Task> TICK_START_QUEUE = new PriorityBlockingQueue<>();
	private static final Queue<Task> TICK_END_QUEUE = new PriorityBlockingQueue<>();
	private static volatile long tick = 0;

	private TaskUtil() {}

	public static void preServerTick() {
		tick++;
		final long t = tick;
		for (int i = TICK_START_QUEUE.size(); i > 0; i--) {
			final Task task = TICK_START_QUEUE.element();
			if (task.tick() > t) {
				return;
			}
			TICK_START_QUEUE.remove();
			task.task().run();
		}
	}

	public static void postServerTick() {
		final long t = tick;
		for (int i = TICK_START_QUEUE.size(); i > 0; i--) {
			final Task task = TICK_START_QUEUE.element();
			if (task.tick() > t) {
				return;
			}
			TICK_START_QUEUE.remove();
			task.task().run();
		}
	}

	public static void queueTickStart(final Runnable task) {
		queueTickStart(0, task);
	}

	public static void queueTickStart(final int delay, final Runnable task) {
		TICK_START_QUEUE.add(new Task(tick + delay, task));
	}

	public static void queueTickEnd(final Runnable task) {
		queueTickStart(0, task);
	}

	public static void queueTickEnd(final int delay, final Runnable task) {
		TICK_END_QUEUE.add(new Task(tick + delay, task));
	}

	record Task(long tick, Runnable task) implements Comparable<Task> {
		@Override
		public int compareTo(final Task other) {
			return this.tick < other.tick ? -1 : this.tick > other.tick ? 1 : 0;
		}
	}
}
