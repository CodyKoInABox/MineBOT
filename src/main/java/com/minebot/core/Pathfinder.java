package com.minebot.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Small 4-adjacent BFS. Walks, jumps one, or drops up to three. */
public final class Pathfinder {
	public enum Kind {
		WALK,
		JUMP,
		DROP
	}

	public record Move(int x, int y, int z, Kind kind) {}

	public interface Collision {
		boolean solid(int x, int y, int z);

		default boolean canStand(int x, int y, int z) {
			return solid(x, y - 1, z) && !solid(x, y, z) && !solid(x, y + 1, z);
		}
	}

	private record Node(int x, int y, int z) {}

	private Pathfinder() {}

	public static List<Move> find(int sx, int sy, int sz, int gx, int gy, int gz, Collision world, int limit) {
		if (sx == gx && sy == gy && sz == gz) {
			return List.of();
		}
		ArrayDeque<Node> queue = new ArrayDeque<>();
		Map<Node, Node> parent = new HashMap<>();
		Map<Node, Kind> how = new HashMap<>();
		Node start = new Node(sx, sy, sz);
		queue.add(start);
		parent.put(start, null);
		int seen = 0;
		int cap = Math.max(1, limit);
		int[][] horizontal = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		Node goal = null;
		while (!queue.isEmpty() && seen < cap) {
			Node node = queue.removeFirst();
			seen++;
			if (node.x == gx && node.y == gy && node.z == gz) {
				goal = node;
				break;
			}
			for (int[] delta : horizontal) {
				int nx = node.x + delta[0];
				int nz = node.z + delta[1];
				offer(queue, parent, how, world, node, nx, node.y, nz, Kind.WALK);
				offer(queue, parent, how, world, node, nx, node.y + 1, nz, Kind.JUMP);
				for (int drop = 1; drop <= 3; drop++) {
					if (!clearDrop(world, nx, node.y, nz, drop)) {
						break;
					}
					offer(queue, parent, how, world, node, nx, node.y - drop, nz, Kind.DROP);
				}
			}
		}
		if (goal == null) {
			return List.of();
		}
		List<Move> reversed = new ArrayList<>();
		Node cursor = goal;
		while (!cursor.equals(start)) {
			reversed.add(new Move(cursor.x, cursor.y, cursor.z, how.get(cursor)));
			cursor = parent.get(cursor);
		}
		Collections.reverse(reversed);
		return reversed;
	}

	private static void offer(
			ArrayDeque<Node> queue,
			Map<Node, Node> parent,
			Map<Node, Kind> how,
			Collision world,
			Node from,
			int x,
			int y,
			int z,
			Kind kind
	) {
		Node next = new Node(x, y, z);
		if (parent.containsKey(next) || !world.canStand(x, y, z)) {
			return;
		}
		if (kind == Kind.JUMP && world.solid(from.x, from.y + 2, from.z)) {
			return;
		}
		parent.put(next, from);
		how.put(next, kind);
		queue.add(next);
	}

	private static boolean clearDrop(Collision world, int x, int fromY, int z, int drop) {
		for (int y = fromY - 1; y > fromY - drop; y--) {
			if (world.solid(x, y, z) || world.solid(x, y + 1, z)) {
				return false;
			}
		}
		return true;
	}
}
