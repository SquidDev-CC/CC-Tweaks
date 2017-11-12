package org.squiddev.cctweaks.pocket;

import com.google.common.collect.Maps;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.squiddev.cctweaks.CCTweaks;
import org.squiddev.cctweaks.api.IDataCard;
import org.squiddev.cctweaks.api.network.INetworkCompatiblePeripheral;
import org.squiddev.cctweaks.core.Config;
import org.squiddev.cctweaks.core.network.bridge.NetworkBindingWithModem;
import org.squiddev.cctweaks.core.network.modem.BasicModemPeripheral;
import org.squiddev.cctweaks.core.network.modem.DynamicPeripheralCollection;
import org.squiddev.cctweaks.core.registry.IModule;
import org.squiddev.cctweaks.core.registry.Registry;
import org.squiddev.cctweaks.core.utils.EntityPosition;

import javax.annotation.Nonnull;
import java.util.Map;

public class PocketWirelessBridge implements IModule, IPocketUpgrade {
	@Nonnull
	@Override
	public ResourceLocation getUpgradeID() {
		return new ResourceLocation(CCTweaks.ID, "wirelessBridge");
	}

	@Nonnull
	@Override
	public String getUnlocalisedAdjective() {
		return "pocket." + CCTweaks.ID + ".wirelessBridge.adjective";
	}

	@Nonnull
	@Override
	public ItemStack getCraftingItem() {
		return Config.Network.WirelessBridge.pocketEnabled ? new ItemStack(Registry.blockNetworked, 1, 0) : ItemStack.EMPTY;
	}

	@Override
	public IPeripheral createPeripheral(@Nonnull IPocketAccess access) {
		return Config.Network.WirelessBridge.pocketEnabled ? new PocketBinding(access).getModem().modem : null;
	}

	@Override
	public void update(@Nonnull IPocketAccess access, IPeripheral peripheral) {
		if (Config.Network.WirelessBridge.pocketEnabled && peripheral instanceof PocketBinding.PocketModemPeripheral) {
			PocketBinding binding = ((PocketBinding.PocketModemPeripheral) peripheral).getBinding();

			binding.update(); // Update entity and save
		}
	}

	@Override
	public boolean onRightClick(@Nonnull World world, @Nonnull IPocketAccess access, IPeripheral peripheral) {
		return false;
	}

	public static class PocketBinding extends NetworkBindingWithModem {
		public final IPocketAccess pocket;

		public PocketBinding(IPocketAccess pocket) {
			super(new EntityPosition(pocket.getEntity()));
			this.pocket = pocket;
		}

		@Override
		public BindingModem createModem() {
			return new PocketModem();
		}

		@Override
		public PocketModem getModem() {
			return (PocketModem) modem;
		}

		@Override
		public void markDirty() {
			save();
		}

		@Override
		public void connect() {
			load(pocket.getUpgradeNBTData());
			super.connect();
		}

		public void save() {
			save(pocket.getUpgradeNBTData());
			pocket.updateUpgradeNBTData();

			pocket.setLight(modem.isActive() ? 0xBA0000 : -1);
		}

		public void update() {
			((EntityPosition) getPosition()).entity = pocket.getEntity();

			// We may receive update events whilst not being attached. To prevent this, just exit if we
			// have no network
			if (getAttachedNetwork() == null) return;

			modem.updateEnabled();
			if (getModem().modem.pollChanged()) save();
		}

		/**
		 * Custom modem that allows modifying bindings
		 */
		public class PocketModem extends BindingModem {
			protected final DynamicPeripheralCollection<ResourceLocation> peripherals = new DynamicPeripheralCollection<ResourceLocation>() {
				@Override
				protected Map<ResourceLocation, IPeripheral> getPeripherals() {
					Map<ResourceLocation, IPeripheral> peripherals = Maps.newHashMap();
					for (Map.Entry<ResourceLocation, IPeripheral> entry : pocket.getUpgrades().entrySet()) {
						if (entry.getValue() instanceof INetworkCompatiblePeripheral) {
							peripherals.put(entry.getKey(), entry.getValue());
						}
					}

					return peripherals;
				}

				@Override
				protected World getWorld() {
					Entity entity = pocket.getEntity();
					return entity == null ? null : entity.getEntityWorld();
				}
			};

			@Nonnull
			@Override
			public Map<String, IPeripheral> getConnectedPeripherals() {
				return peripherals.getConnectedPeripherals();
			}

			@Override
			protected BasicModemPeripheral<?> createPeripheral() {
				return new PocketModemPeripheral(this);
			}
		}

		/**
		 * Calls {@link PocketBinding#connect()} and {@link PocketBinding#destroy()} on attach and detach.
		 */
		public class PocketModemPeripheral extends BindingModemPeripheral {
			public PocketModemPeripheral(NetworkBindingWithModem.BindingModem modem) {
				super(modem);
			}

			@Nonnull
			@Override
			public String[] getMethodNames() {
				String[] methods = super.getMethodNames();
				String[] newMethods = new String[methods.length + 2];
				System.arraycopy(methods, 0, newMethods, 0, methods.length);


				int l = methods.length;
				newMethods[l] = "bindFromCard";
				newMethods[l + 1] = "bindToCard";

				return newMethods;
			}

			@Override
			public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] arguments) throws LuaException, InterruptedException {
				String[] methods = super.getMethodNames();
				switch (method - methods.length) {
					case 0: { // bindFromCard
						if (!(pocket.getEntity() instanceof EntityPlayer)) {
							return new Object[]{false, "No inventory found"};
						}
						InventoryPlayer inventory = ((EntityPlayer) pocket.getEntity()).inventory;

						int size = inventory.getSizeInventory(), held = inventory.currentItem;
						for (int i = 0; i < size; i++) {
							ItemStack stack = inventory.getStackInSlot((i + held) % size);
							if (!stack.isEmpty() && stack.getItem() instanceof IDataCard) {
								IDataCard card = (IDataCard) stack.getItem();
								if (PocketBinding.this.load(stack, card)) {
									PocketBinding.this.save();
									return new Object[]{true};
								}
							}
						}

						return new Object[]{false, "No card found"};
					}
					case 1: { // bindToCard
						if (!(pocket.getEntity() instanceof EntityPlayer)) {
							return new Object[]{false, "No inventory found"};
						}
						InventoryPlayer inventory = ((EntityPlayer) pocket.getEntity()).inventory;

						int size = inventory.getSizeInventory(), held = inventory.currentItem;
						for (int i = 0; i < size; i++) {
							ItemStack stack = inventory.getStackInSlot((i + held) % size);
							if (!stack.isEmpty() && stack.getItem() instanceof IDataCard) {
								IDataCard card = (IDataCard) stack.getItem();
								PocketBinding.this.save(stack, card);
								return new Object[]{true};
							}
						}

						return new Object[]{false, "No card found"};
					}
				}

				return super.callMethod(computer, context, method, arguments);
			}

			@Override
			public synchronized void attach(@Nonnull IComputerAccess computer) {
				PocketBinding.this.connect();
				super.attach(computer);
			}

			@Override
			public synchronized void detach(@Nonnull IComputerAccess computer) {
				super.detach(computer);
				PocketBinding.this.destroy();
			}

			public PocketBinding getBinding() {
				return PocketBinding.this;
			}
		}
	}

	@Override
	public void preInit() {
		ComputerCraft.registerPocketUpgrade(this);
	}
}
