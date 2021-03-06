package org.squiddev.cctweaks.core.network.modem;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for remote peripherals
 */
public class PeripheralAccess implements IComputerAccess {
	private final IPeripheral peripheral;
	private final IComputerAccess computer;
	private final String name;

	private final String[] methods;
	private final Map<String, Integer> methodMap;

	public PeripheralAccess(IPeripheral peripheral, IComputerAccess computer, String name) {
		this.peripheral = peripheral;
		this.computer = computer;
		this.name = name;

		String[] methods = peripheral.getMethodNames();
		if (peripheral.getType() == null || methods == null) {
			throw new RuntimeException("Peripheral " + peripheral + " provides no name or methods");
		}

		this.methods = methods;
		Map<String, Integer> map = methodMap = new HashMap<String, Integer>();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i] != null) map.put(methods[i], i);
		}
	}

	public void attach() {
		peripheral.attach(this);
		computer.queueEvent("peripheral", new Object[]{getAttachmentName()});
	}

	public void detach() {
		peripheral.detach(this);
		computer.queueEvent("peripheral_detach", new Object[]{getAttachmentName()});
	}

	public String getType() {
		return peripheral.getType();
	}

	public String[] getMethodNames() {
		return methods;
	}

	public Object[] callMethod(ILuaContext context, String methodName, Object[] arguments) throws InterruptedException, LuaException {
		Integer method = methodMap.get(methodName);
		if (method != null) return peripheral.callMethod(this, context, method, arguments);

		throw new LuaException("No such method " + methodName);
	}

	@Override
	public String mount(@Nonnull String desiredLocation, @Nonnull IMount mount) {
		return computer.mount(desiredLocation, mount, name);
	}

	@Override
	public String mount(@Nonnull String desiredLocation, @Nonnull IMount mount, @Nonnull String driveName) {
		return computer.mount(desiredLocation, mount, driveName);
	}

	@Override
	public String mountWritable(@Nonnull String desiredLocation, @Nonnull IWritableMount mount) {
		return computer.mountWritable(desiredLocation, mount, name);
	}

	@Override
	public String mountWritable(@Nonnull String desiredLocation, @Nonnull IWritableMount mount, @Nonnull String driveName) {
		return computer.mountWritable(desiredLocation, mount, driveName);
	}

	@Override
	public void unmount(String location) {
		computer.unmount(location);
	}

	@Override
	public int getID() {
		return computer.getID();
	}

	@Override
	public void queueEvent(@Nonnull String event, Object[] arguments) {
		computer.queueEvent(event, arguments);
	}

	@Nonnull
	@Override
	public String getAttachmentName() {
		return name;
	}
}
