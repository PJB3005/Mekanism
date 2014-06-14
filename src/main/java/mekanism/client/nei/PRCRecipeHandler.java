package mekanism.client.nei;

import static codechicken.lib.gui.GuiDraw.changeTexture;
import static codechicken.lib.gui.GuiDraw.drawTexturedModalRect;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mekanism.api.PressurizedReactants;
import mekanism.api.PressurizedRecipe;
import mekanism.api.gas.GasStack;
import mekanism.client.gui.GuiElement;
import mekanism.client.gui.GuiPRC;
import mekanism.client.gui.GuiPowerBar;
import mekanism.client.gui.GuiPowerBar.IPowerInfoHandler;
import mekanism.client.gui.GuiProgress;
import mekanism.client.gui.GuiProgress.IProgressInfoHandler;
import mekanism.client.gui.GuiProgress.ProgressBar;
import mekanism.client.gui.GuiSlot;
import mekanism.client.gui.GuiSlot.SlotOverlay;
import mekanism.client.gui.GuiSlot.SlotType;
import mekanism.client.nei.RotaryCondensentratorRecipeHandler.CachedIORecipe;
import mekanism.common.ObfuscatedNames;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.NEIServerUtils;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

public class PRCRecipeHandler extends BaseRecipeHandler
{
	private int ticksPassed;
	
	public static int xOffset = 5;
	public static int yOffset = 3;
	
	@Override
	public void addGuiElements()
	{
		guiElements.add(new GuiSlot(SlotType.INPUT, this, MekanismUtils.getResource(ResourceType.GUI, getGuiTexture()), 53, 34));
		guiElements.add(new GuiSlot(SlotType.POWER, this, MekanismUtils.getResource(ResourceType.GUI, getGuiTexture()), 140, 18).with(SlotOverlay.POWER));
		guiElements.add(new GuiSlot(SlotType.OUTPUT, this, MekanismUtils.getResource(ResourceType.GUI, getGuiTexture()), 115, 34));

		guiElements.add(new GuiPowerBar(this, new IPowerInfoHandler() {
			@Override
			public double getLevel()
			{
				return ticksPassed <= 20 ? ticksPassed / 20.0F : 1.0F;
			}
		}, MekanismUtils.getResource(ResourceType.GUI, getGuiTexture()), 164, 15));
		guiElements.add(new GuiProgress(new IProgressInfoHandler()
		{
			@Override
			public double getProgress()
			{
				return ticksPassed >= 20 ? (ticksPassed - 20) % 20 / 20.0F : 0.0F;
			}
		}, getProgressType(), this, MekanismUtils.getResource(ResourceType.GUI, getGuiTexture()), 75, 37));
	}
	
	public ProgressBar getProgressType()
	{
		return ProgressBar.MEDIUM;
	}
	
	public Set<Entry<PressurizedReactants, PressurizedRecipe>> getRecipes()
	{
		return Recipe.PRESSURIZED_REACTION_CHAMBER.get().entrySet();
	}
	
	@Override
	public void drawBackground(int i)
	{
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		changeTexture(getGuiTexture());
		drawTexturedModalRect(-2, 0, 3, yOffset, 170, 80);
		
		for(GuiElement e : guiElements)
		{
			e.renderBackground(0, 0, xOffset, yOffset);
		}
	}
	
	@Override
	public void loadTransferRects()
	{
		transferRects.add(new TemplateRecipeHandler.RecipeTransferRect(new Rectangle(70, 34, 36, 10), getRecipeId(), new Object[0]));
	}
	
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		ticksPassed++;
	}
	
	@Override
	public String getRecipeName() 
	{
		return MekanismUtils.localize("tile.MachineBlock2.PressurizedReactionChamber.name");
	}
	
	@Override
	public Class getGuiClass()
	{
		return GuiPRC.class;
	}
	
	@Override
	public String getOverlayIdentifier()
	{
		return "prc";
	}
	
	@Override
	public int recipiesPerPage()
	{
		return 1;
	}
	
	public String getRecipeId()
	{
		return "mekanism.prc";
	}
	
	@Override
	public String getGuiTexture() 
	{
		return "nei/GuiPRC.png";
	}
	
	@Override
	public void drawExtras(int i)
	{
		CachedIORecipe recipe = (CachedIORecipe)arecipes.get(i);

		drawTexturedModalRect(47-xOffset, 39-yOffset, 176, 71, 28, 8);
		drawTexturedModalRect(101-xOffset, 39-yOffset, 176, 63, 28, 8);
		
		if(recipe.pressurizedRecipe.reactants.getFluid() != null)
		{
			displayGauge(58, 26-xOffset, 14-yOffset, 176, 4, 58, recipe.pressurizedRecipe.reactants.getFluid(), null);
		}

		if(recipe.pressurizedRecipe.reactants.getGas() != null)
		{
			displayGauge(58, 26-xOffset, 14-yOffset, 176, 4, 58, null, recipe.pressurizedRecipe.reactants.getGas());
		}

		if(recipe.pressurizedRecipe.products.getGasOutput() != null)
		{
			displayGauge(58, 80-xOffset, 5-yOffset, 176, 4, 58, null, recipe.pressurizedRecipe.products.getGasOutput());
		}
	}
	
	@Override
	public void loadCraftingRecipes(String outputId, Object... results)
	{
		if(outputId.equals(getRecipeId()))
		{
			for(Map.Entry irecipe : getRecipes())
			{
				arecipes.add(new CachedIORecipe(irecipe));
			}
		}
		else if(outputId.equals("gas") && results.length == 1 && results[0] instanceof GasStack)
		{
			for(Map.Entry<PressurizedReactants, PressurizedRecipe> irecipe : getRecipes())
			{
				if(irecipe.getValue().reactants.containsType((GasStack)results[0]))
				{
					arecipes.add(new CachedIORecipe(irecipe));
				}
			}
		}
		else {
			super.loadCraftingRecipes(outputId, results);
		}
	}
	
	@Override
	public void loadCraftingRecipes(ItemStack result)
	{
		for(Map.Entry<PressurizedReactants, PressurizedRecipe> irecipe : getRecipes())
		{
			if(NEIServerUtils.areStacksSameTypeCrafting(irecipe.getValue().products.getItemOutput(), result))
			{
				arecipes.add(new CachedIORecipe(irecipe));
			}
		}
	}

	@Override
	public void loadUsageRecipes(String inputId, Object... ingredients)
	{
		if(inputId.equals("gas") && ingredients.length == 1 && ingredients[0] instanceof GasStack)
		{
			for(Map.Entry<PressurizedReactants, PressurizedRecipe> irecipe : getRecipes())
			{
				if(irecipe.getKey().containsType((GasStack)ingredients[0]))
				{
					arecipes.add(new CachedIORecipe(irecipe));
				}
			}
		}
		else if(inputId.equals("fluid") && ingredients.length == 1 && ingredients[0] instanceof FluidStack)
		{
			for(Map.Entry<PressurizedReactants, PressurizedRecipe> irecipe : getRecipes())
			{
				if(irecipe.getKey().containsType((FluidStack)ingredients[0]))
				{
					arecipes.add(new CachedIORecipe(irecipe));
				}
			}
		}
		else {
			super.loadUsageRecipes(inputId, ingredients);
		}
	}
	
	@Override
	public void loadUsageRecipes(ItemStack ingredient)
	{
		for(Map.Entry<PressurizedReactants, PressurizedRecipe> irecipe : getRecipes())
		{
			if(NEIServerUtils.areStacksSameTypeCrafting(irecipe.getKey().getSolid(), ingredient))
			{
				arecipes.add(new CachedIORecipe(irecipe));
			}
		}
	}

	@Override
	public List<String> handleTooltip(GuiRecipe gui, List<String> currenttip, int recipe)
	{
		Point point = GuiDraw.getMousePosition();

		int xAxis = point.x-(Integer)MekanismUtils.getPrivateValue(gui, GuiContainer.class, ObfuscatedNames.GuiContainer_guiLeft);
		int yAxis = point.y-(Integer)MekanismUtils.getPrivateValue(gui, GuiContainer.class, ObfuscatedNames.GuiContainer_guiTop);

		if(xAxis >= 80 && xAxis <= 96 && yAxis >= 5+13 && yAxis <= 63+13)
		{
			currenttip.add(((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.reactants.getFluid().getFluid().getLocalizedName());
		}
		else if(xAxis >= 26 && xAxis <= 42 && yAxis >= 14+13 && yAxis <= 72+13)
		{
			currenttip.add(((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.reactants.getGas().getGas().getLocalizedName());
		}
		else if(xAxis >= 134 && xAxis <= 150 && yAxis >= 14+13 && yAxis <= 72+13)
		{
			currenttip.add(((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.products.getGasOutput().getGas().getLocalizedName());
		}

		return super.handleTooltip(gui, currenttip, recipe);
	}
	
	@Override
	public boolean keyTyped(GuiRecipe gui, char keyChar, int keyCode, int recipe)
	{
		Point point = GuiDraw.getMousePosition();

		int xAxis = point.x-(Integer)MekanismUtils.getPrivateValue(gui, GuiContainer.class, ObfuscatedNames.GuiContainer_guiLeft);
		int yAxis = point.y-(Integer)MekanismUtils.getPrivateValue(gui, GuiContainer.class, ObfuscatedNames.GuiContainer_guiTop);

		GasStack gas = null;
		FluidStack fluid = null;

		if(xAxis >= 80 && xAxis <= 96 && yAxis >= 5+13 && yAxis <= 63+13)
		{
			fluid = ((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.reactants.getFluid();
		}
		else if(xAxis >= 26 && xAxis <= 42 && yAxis >= 14+13 && yAxis <= 72+13)
		{
			gas = ((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.reactants.getGas();
		}
		else if(xAxis >= 134 && xAxis <= 150 && yAxis >= 14+13 && yAxis <= 72+13)
		{
			gas = ((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.products.getGasOutput();
		}

		if(gas != null)
		{
			if(keyCode == NEIClientConfig.getKeyBinding("gui.recipe"))
			{
				if(doGasLookup(gas, false))
				{
					return true;
				}
			}
			else if(keyCode == NEIClientConfig.getKeyBinding("gui.usage"))
			{
				if(doGasLookup(gas, true))
				{
					return true;
				}
			}
		}
		else if(fluid != null)
		{
			if(keyCode == NEIClientConfig.getKeyBinding("gui.recipe"))
			{
				if(doFluidLookup(fluid, false))
				{
					return true;
				}
			}
			else if(keyCode == NEIClientConfig.getKeyBinding("gui.usage"))
			{
				if(doFluidLookup(fluid, true))
				{
					return true;
				}
			}
		}

		return super.keyTyped(gui, keyChar, keyCode, recipe);
	}
	
	@Override
	public boolean mouseClicked(GuiRecipe gui, int button, int recipe)
	{
		Point point = GuiDraw.getMousePosition();

		int xAxis = point.x - (Integer)MekanismUtils.getPrivateValue(gui, GuiContainer.class, ObfuscatedNames.GuiContainer_guiLeft);
		int yAxis = point.y - (Integer)MekanismUtils.getPrivateValue(gui, GuiContainer.class, ObfuscatedNames.GuiContainer_guiTop);

		GasStack gas = null;
		FluidStack fluid = null;

		if(xAxis >= 80 && xAxis <= 96 && yAxis >= 5+13 && yAxis <= 63+13)
		{
			fluid = ((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.reactants.getFluid();
		}
		else if(xAxis >= 26 && xAxis <= 42 && yAxis >= 14+13 && yAxis <= 72+13)
		{
			gas = ((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.reactants.getGas();
		}
		else if(xAxis >= 134 && xAxis <= 150 && yAxis >= 14+13 && yAxis <= 72+13)
		{
			gas = ((CachedIORecipe)arecipes.get(recipe)).pressurizedRecipe.products.getGasOutput();
		}

		if(gas != null)
		{
			if(button == 0)
			{
				if(doGasLookup(gas, false))
				{
					return true;
				}
			}
			else if(button == 1)
			{
				if(doGasLookup(gas, true))
				{
					return true;
				}
			}
		}
		else if(fluid != null)
		{
			if(button == 0)
			{
				if(doFluidLookup(fluid, false))
				{
					return true;
				}
			}
			else if(button == 1)
			{
				if(doFluidLookup(fluid, true))
				{
					return true;
				}
			}
		}

		return super.mouseClicked(gui, button, recipe);
	}
	
	public class CachedIORecipe extends TemplateRecipeHandler.CachedRecipe
	{
		public PressurizedRecipe pressurizedRecipe;
		
		public PositionedStack input;
		public PositionedStack output;

		@Override
		public PositionedStack getIngredient()
		{
			return input;
		}

		@Override
		public PositionedStack getResult()
		{
			return output;
		}

		public CachedIORecipe(PressurizedRecipe recipe)
		{
			super();
			
			pressurizedRecipe = recipe;
			
			input = new PositionedStack(recipe.reactants.getSolid(), 54, 35);
			output = new PositionedStack(recipe.products.getItemOutput(), 116, 35);
		}

		public CachedIORecipe(Map.Entry recipe)
		{
			this((PressurizedRecipe)recipe.getValue());
		}
	}
}