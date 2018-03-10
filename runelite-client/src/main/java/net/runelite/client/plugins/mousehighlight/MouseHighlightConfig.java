/*
 * Copyright (c) 2018 Charlie Waters
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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.awt.*;

@ConfigGroup(
	keyName = "mousehighlight",
	name = "Mouse Tooltips",
	description = "Configuration for the Mouse Tooltips plugin"
)
public interface MouseHighlightConfig extends Config
{
	@ConfigItem(
		keyName = "showGEPrice",
		name = "Show Grand Exchange Prices",
		description = "Configures whether or not to show GE prices on item tooltips",
		position = 1
	)
	default boolean showGEPrice()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHAValue",
		name = "Show High Alchemy Values",
		description = "Configures whether or not to show High Alchemy values on item tooltips",
		position = 2
	)
	default boolean showHAValue()
	{
		return false;
	}

	@ConfigItem(
		keyName = "defaultColor",
		name = "Default items color",
		description = "Configures the color for default, non-highlighted items",
		position = 3
	)
	default Color defaultColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "lowValueColor",
		name = "Low value items color",
		description = "Configures the color for low value items",
		position = 4
	)
	default Color lowValueColor()
	{
		return Color.decode("#66B2FF");
	}

	@ConfigItem(
		keyName = "mediumValueColor",
		name = "Medium value items color",
		description = "Configures the color for medium value items",
		position = 5
	)
	default Color mediumValueColor()
	{
		return Color.decode("#99FF99");
	}

	@ConfigItem(
		keyName = "highValueColor",
		name = "High value items color",
		description = "Configures the color for high value items",
		position = 6
	)
	default Color highValueColor()
	{
		return Color.decode("#FF9600");
	}

	@ConfigItem(
		keyName = "insaneValueColor",
		name = "Insane value items color",
		description = "Configures the color for insane value items",
		position = 7
	)
	default Color insaneValueColor()
	{
		return Color.decode("#FF66B2");
	}
}
