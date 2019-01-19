/*
 * Copyright (c) 2018, Lotto <https://github.com/devLotto>
 * Copyright (c) 2018, Henke <https://github.com/henke96>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.runedoku;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

@Slf4j
public class RuneDokuOverlay extends Overlay
{

	public static final int ROGUE_TRADER_PUZZLE_GROUP_ID = 288;

	public static class RogueTrader
	{
		public static final int FIRST_TILE = 10;
		public static final int RUNE_GRID = 131;
	}

	private final Client client;
	private final ScheduledExecutorService executorService;
	private final ItemManager itemManager;

	private static final int[] runeList = new int[] { 6436, 6422, 6428, 6424, 6426, 6438, 6432, 6430, 6434 };
	private static final Map<Integer, Integer> runeDokuMap = new HashMap<>();
	private static BufferedImage[] runeSprites;

	static
	{
		for (int i = 0; i < runeList.length; ++i)
		{
			runeDokuMap.put(runeList[i], i + 1);
		}
	}

	@Inject
	public RuneDokuOverlay(Client client, ScheduledExecutorService executorService, ItemManager itemManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		this.client = client;
		this.executorService = executorService;
		this.itemManager = itemManager;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// get rogue trader rune puzzle widget
		Widget runeDoku = client.getWidget(ROGUE_TRADER_PUZZLE_GROUP_ID, RogueTrader.RUNE_GRID);
		if (runeDoku != null)
		{
			if (!runeDoku.isHidden())
			{
				solveRuneDoku(graphics, runeDoku);
			}
		}

		return null;
	}

	private void solveRuneDoku(Graphics2D graphics, Widget widget)
	{
		Collection<WidgetItem> items = widget.getWidgetItems();
		if (items == null)
		{
			return;
		}

		int[] puzzle = new int[81];

		for (WidgetItem item : items)
		{
			final int idx = item.getIndex();
			if (idx > 80)
			{
				log.warn("Invalid WidgetItem index");
				return;
			}
			final int id = item.getId();
			if (runeDokuMap.containsKey(id))
			{
				puzzle[idx] = runeDokuMap.get(id);
			}
		}

		int[] solvedPuzzle = puzzle.clone();
		if (!sudokuSolve(solvedPuzzle))
		{
			log.warn("Failed to solve RuneDoku");
			return;
		}

		if (runeSprites == null)
		{
			// Make all the sprites only the first time
			runeSprites = new BufferedImage[runeList.length];
			for(int i = 0; i < runeSprites.length; ++i)
			{
				BufferedImage sprite = itemManager.getImage(runeList[i]);
				if (sprite == null)
				{
					log.warn("Failed to load sprite");
					return;
				}
				BufferedImage scaledSprite = getScaledImage(sprite, 0.6);
				if (scaledSprite == null)
				{
					log.warn("Failed to load sprite");
					return;
				}

				runeSprites[i] = scaledSprite;
			}
		}

		for (int i = 0 ; i < puzzle.length; ++i)
		{
			if (puzzle[i] != 0)
			{
				// Don't draw sprites on fixed pieces
				continue;
			}

			if (solvedPuzzle[i] == 0)
			{
				// none of the solution should be empty
				log.warn("Bad solution");
				return;
			}

			// grid of widgets child ids are sequential
			Widget w = client.getWidget(ROGUE_TRADER_PUZZLE_GROUP_ID, RogueTrader.FIRST_TILE + i);
			// draw sprites over each tile widget
			OverlayUtil.renderImageLocation(graphics, w.getCanvasLocation(), runeSprites[solvedPuzzle[i] - 1]);
		}
	}

	private BufferedImage getScaledImage(BufferedImage image, double scale)
	{
		AffineTransform transform = new AffineTransform();
		transform.scale(scale, scale);
		AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
		return transformOp.filter(image, null);
	}

	// Ripped from https://gist.github.com/vaskoz/8211276
	public static boolean sudokuSolve(int[] puzzle) {
		int N = (int) Math.round(Math.pow(puzzle.length, 0.25d)); // length ^ 0.25
		int SIZE = N * N;
		int CELLS = SIZE * SIZE;
		boolean noEmptyCells = true;
		int myRow = 0, myCol = 0;
		for (int i = 0; i < CELLS; i++) {
			if (puzzle[i] == 0) {
				myRow = i / SIZE;
				myCol = i % SIZE;
				noEmptyCells = false;
				break;
			}
		}
		if (noEmptyCells) return true;

		for (int choice = 1; choice <= SIZE; choice++) {
			boolean isValid = true;
			int gridRow = myRow / N;
			int gridCol = myCol / N;
			// check grid for duplicates
			for (int row = N * gridRow; row < N * gridRow + N; row++)
				for (int col = N * gridCol; col < N * gridCol + N; col++)
					if (puzzle[row * SIZE + col] == choice)
						isValid = false;

			// row & column
			for (int j = 0; j < SIZE; j++)
				if (puzzle[SIZE * j + myCol] == choice || puzzle[myRow * SIZE + j] == choice) {
					isValid = false;
					break;
				}


			if (isValid) {
				puzzle[myRow * SIZE + myCol] = choice;
				boolean solved = sudokuSolve(puzzle);
				if (solved) return true;
				else puzzle[myRow * SIZE + myCol] = 0;
			}
		}
		return false;
	}
}
