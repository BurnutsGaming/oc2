package li.cil.circuity.vm;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.MemoryRange;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class SimpleMemoryMap implements MemoryMap {
    private final Map<MemoryMappedDevice, MemoryRange> devices = new HashMap<>();

    // For device IO we often get sequential access to the same range/device, so we remember the last one as a cache.
    private MemoryRange cache;

    @Override
    public OptionalInt findFreeRange(final int start, final int end, final int size) {
        if (size == 0) {
            return OptionalInt.empty();
        }

        if (Integer.compareUnsigned(end, start) < 0) {
            return OptionalInt.empty();
        }

        if (Integer.compareUnsigned(end - start, size - 1) < 0) {
            return OptionalInt.empty();
        }

        if (Integer.toUnsignedLong(start) + Integer.toUnsignedLong(size) >= 0xFFFFFFFFL) {
            return OptionalInt.empty();
        }

        final MemoryRange candidateRange = new MemoryRange(null, start, start + size);
        for (final MemoryRange existingRange : devices.values()) {
            if (existingRange.intersects(candidateRange)) {
                if (existingRange.end != 0xFFFFFFFF) { // Avoid overflow.
                    return findFreeRange(existingRange.end + 1, end, size);
                } else {
                    return OptionalInt.empty();
                }
            }
        }

        return OptionalInt.of(start);
    }

    @Override
    public boolean addDevice(final int address, final MemoryMappedDevice device) {
        final MemoryRange deviceRange = new MemoryRange(device, address);
        if (devices.values().stream().anyMatch(range -> range.intersects(deviceRange))) {
            return false;
        }
        devices.put(device, deviceRange);
        return true;
    }

    @Override
    public void removeDevice(final MemoryMappedDevice device) {
        devices.remove(device);
        cache = null;
    }

    @Override
    public int getDeviceAddress(final MemoryMappedDevice device) {
        final MemoryRange range = devices.get(device);
        if (range != null) {
            return range.start;
        } else {
            return -1;
        }
    }

    @Nullable
    @Override
    public MemoryRange getMemoryRange(final int address) {
        // TODO some proper index structure to speed this up?
        if (cache != null && cache.contains(address)) {
            return cache;
        }

        for (final MemoryRange range : devices.values()) {
            if (range.contains(address)) {
                cache = range;
                return range;
            }
        }

        return null;
    }

    @Override
    public void setDirty(final MemoryRange range, final int offset) {
        // todo implement tracking dirty bits; really need this if we want to add a frame buffer.
    }

    @Override
    public byte load8(final int address) throws MemoryAccessException {
        final MemoryRange range = getMemoryRange(address);
        if (range != null) {
            return range.device.load8(address - range.start);
        }
        return 0;
    }

    @Override
    public void store8(final int address, final byte value) throws MemoryAccessException {
        final MemoryRange range = getMemoryRange(address);
        if (range != null) {
            range.device.store8(address - range.start, value);
        }
    }

    @Override
    public short load16(final int address) throws MemoryAccessException {
        final MemoryRange range = getMemoryRange(address);
        if (range != null) {
            return range.device.load16(address - range.start);
        }
        return 0;
    }

    @Override
    public void store16(final int address, final short value) throws MemoryAccessException {
        final MemoryRange range = getMemoryRange(address);
        if (range != null) {
            range.device.store16(address - range.start, value);
        }
    }

    @Override
    public int load32(final int address) throws MemoryAccessException {
        final MemoryRange range = getMemoryRange(address);
        if (range != null) {
            return range.device.load32(address - range.start);
        }
        return 0;
    }

    @Override
    public void store32(final int address, final int value) throws MemoryAccessException {
        final MemoryRange range = getMemoryRange(address);
        if (range != null) {
            range.device.store32(address - range.start, value);
        }
    }
}