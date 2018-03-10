/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.mousehighlight;

import com.google.common.base.Strings;

import java.awt.*;
import java.awt.Point;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.queries.BankItemQuery;
import net.runelite.api.queries.InventoryItemQuery;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.StackFormatter;
import net.runelite.http.api.item.ItemPrice;

@Slf4j
class MouseHighlightOverlay extends Overlay
{
	// Threshold for highlighting items as blue.
	private static final int LOW_VALUE = 20_000;
	// Threshold for highlighting items as green.
	private static final int MEDIUM_VALUE = 100_000;
	// Threshold for highlighting items as amber.
	private static final int HIGH_VALUE = 1_000_000;
	// Threshold for highlighting items as pink.
	private static final int INSANE_VALUE = 10_000_000;
	// Used when getting High Alchemy value - multiplied by general store price.
	private static final float HIGH_ALCHEMY_CONSTANT = 0.6f;

	private final Client client;
	private final MouseHighlightConfig config;
	private final TooltipManager tooltipManager;
	private final StringBuilder itemStringBuilder = new StringBuilder();

	@Inject
	ItemManager itemManager;

	@Inject
	MouseHighlightOverlay(Client client, MouseHighlightConfig config, TooltipManager tooltipManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		this.client = client;
		this.config = config;
		this.tooltipManager = tooltipManager;
	}

	@Override
	public Dimension render(Graphics2D graphics, Point point)
	{
		if (client.isMenuOpen())
		{
			return null;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		int last = menuEntries.length - 1;

		if (last < 0)
		{
			return null;
		}

		MenuEntry menuEntry = menuEntries[last];
		String target = menuEntry.getTarget();
		String option = menuEntry.getOption();
		MenuAction action = menuEntry.getType();
		int widgetid = menuEntry.getParam1();

		if (Strings.isNullOrEmpty(option))
		{
			return null;
		}

		// Trivial options that don't need to be highlighted, add more as they appear.
		switch (option)
		{
			case "Walk here":
			case "Cancel":
			case "Continue":
				return null;
			case "Move":
				// Hide overlay on sliding puzzle boxes
				if (target.contains("Sliding piece"))
				{
					return null;
				}
		}

		String extra = null;

		// Inventory
		if (widgetid == WidgetInfo.INVENTORY.getPackedId() ||
			widgetid == WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER.getPackedId())
		{
			int index = menuEntry.getParam0();
			Item[] invitems = new InventoryItemQuery().result(client);
			if (index < invitems.length)
			{
				final int id = invitems[index].getId();
				final int qty = invitems[index].getQuantity();
				extra = getStackValue(id, qty);
			}
		}
		// Bank
		else if (widgetid == WidgetInfo.BANK_ITEM_CONTAINER.getPackedId())
		{
			int index = menuEntry.getParam0();
			WidgetItem[] bankitems = new BankItemQuery().result(client);
			if (index < bankitems.length)
			{
				final int id = bankitems[index].getId();
				final int qty = bankitems[index].getQuantity();
				extra = getStackValue(id, qty);
			}
		}
		// Ground
		else if (action == MenuAction.GROUND_ITEM_THIRD_OPTION)
		{
			log.info("Ground " + menuEntry.toString());
			final int id = menuEntry.getIdentifier();
			final int qty = 1;
			extra = getStackValue(id, qty);
		}

		tooltipManager.add(new Tooltip(option +
				(Strings.isNullOrEmpty(target) ? "" : " " + target) +
				(Strings.isNullOrEmpty(extra) ? "" : "</br>" + extra)
		));
		return null;
	}

	private String getStackValue(int id, int qty)
	{
		if (id == ItemID.COINS_995)
		{
			return getValueColorMarkup(qty) + StackFormatter.quantityToRSStackSize(qty);
		}
		else if (id == ItemID.PLATINUM_TOKEN)
		{
			return getValueColorMarkup(qty * 1000) + StackFormatter.quantityToRSStackSize(qty * 1000);
		}

		ItemComposition item = itemManager.getItemComposition(id);
		if (item.isTradable() && config.showGEPrice())
		{
			ItemPrice price = itemManager.getItemPriceAsync(id);
			if (price != null)
			{
				if (config.showHAValue())
				{
					return stackValueText(qty, price.getPrice(), Math.round(item.getPrice() * HIGH_ALCHEMY_CONSTANT));
				}
				else
				{
					return stackValueText(qty, price.getPrice(), 0);
				}
			}
		} else if (config.showHAValue()){
			return stackValueText(qty, 0, Math.round(item.getPrice() * HIGH_ALCHEMY_CONSTANT));
		}
		return null;
	}

	private String stackValueText(int qty, int gePrice, int haValue)
	{
		if (qty <= 1)
		{
			if (gePrice > 0)
			{
				itemStringBuilder.append(getValueColorMarkup(gePrice))
						.append(" (EX: ")
						.append(StackFormatter.quantityToStackSize(gePrice))
						.append(" gp)");
			}
			if (haValue > 0)
			{
				itemStringBuilder.append(getValueColorMarkup(haValue))
						.append(" (HA: ")
						.append(StackFormatter.quantityToStackSize(haValue))
						.append(" gp)");
			}
		}
		else
		{
			if (gePrice > 0 || haValue > 0)
			{
				itemStringBuilder.append(" <col=ffffff>").append(qty);
			}
			if (gePrice > 0)
			{
				itemStringBuilder.append(getValueColorMarkup(gePrice * qty))
						.append(" (EX: ")
						.append(StackFormatter.quantityToStackSize(gePrice * qty))
						.append(" gp)");
			}
			if (haValue > 0)
			{
				itemStringBuilder.append(getValueColorMarkup(haValue * qty))
						.append(" (HA: ")
						.append(StackFormatter.quantityToStackSize(haValue * qty))
						.append(" gp)");
			}
		}

		final String text = itemStringBuilder.toString();
		itemStringBuilder.setLength(0);
		return text;
	}

	private String getValueColorMarkup(int value)
	{
		Color textColor = config.defaultColor();

		// set the color according to value
		if (value >= INSANE_VALUE) // 10,000,000 gp
		{
			textColor = config.insaneValueColor();
		}
		else if (value >= HIGH_VALUE) // 1,000,000 gp
		{
			textColor = config.highValueColor();
		}
		else if (value >= MEDIUM_VALUE) // 100,000 gp
		{
			textColor = config.mediumValueColor();
		}
		else if (value >= LOW_VALUE) // 20,000 gp
		{
			textColor = config.lowValueColor();
		}

		return "<col=" + Integer.toHexString(textColor.getRGB()).substring(2) + ">";
	}
}
