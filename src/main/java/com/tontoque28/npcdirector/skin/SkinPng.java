package com.tontoque28.npcdirector.skin;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

public final class SkinPng {

	public static final int MAX_BYTES = 16384;
	public static final int WIDTH = 64;

	public enum Problem {
		NONE, TOO_BIG, NOT_PNG, DIMENSIONS, CORRUPT
	}

	private static final byte[] MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

	private SkinPng() {
	}

	private static int int32(byte[] data, int pos) {
		return ((data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16) | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
	}

	public static Problem check(byte[] data) {
		if (data == null || data.length < 57) {
			return Problem.NOT_PNG;
		}
		if (data.length > MAX_BYTES) {
			return Problem.TOO_BIG;
		}
		for (int i = 0; i < MAGIC.length; i++) {
			if (data[i] != MAGIC[i]) {
				return Problem.NOT_PNG;
			}
		}
		int pos = 8;
		boolean header = false;
		boolean pixels = false;
		boolean ended = false;
		int width = 0;
		int height = 0;
		while (pos + 12 <= data.length) {
			int length = int32(data, pos);
			if (length < 0 || length > data.length - pos - 12) {
				return Problem.CORRUPT;
			}
			byte[] type = {data[pos + 4], data[pos + 5], data[pos + 6], data[pos + 7]};
			for (byte letter : type) {
				if (!((letter >= 'A' && letter <= 'Z') || (letter >= 'a' && letter <= 'z'))) {
					return Problem.CORRUPT;
				}
			}
			CRC32 crc = new CRC32();
			crc.update(data, pos + 4, length + 4);
			if ((int) crc.getValue() != int32(data, pos + 8 + length)) {
				return Problem.CORRUPT;
			}
			String name = new String(type, StandardCharsets.US_ASCII);
			if (!header) {
				if (!name.equals("IHDR") || length != 13) {
					return Problem.NOT_PNG;
				}
				width = int32(data, pos + 8);
				height = int32(data, pos + 12);
				int depth = data[pos + 16] & 0xFF;
				int color = data[pos + 17] & 0xFF;
				int interlace = data[pos + 20] & 0xFF;
				if (width != WIDTH || (height != 64 && height != 32)) {
					return Problem.DIMENSIONS;
				}
				boolean colorOk = color == 0 || color == 2 || color == 3 || color == 4 || color == 6;
				boolean depthOk = depth == 1 || depth == 2 || depth == 4 || depth == 8 || depth == 16;
				if (!colorOk || !depthOk || interlace > 1) {
					return Problem.CORRUPT;
				}
				header = true;
			} else if (name.equals("zTXt") || name.equals("iTXt") || name.equals("iCCP") || name.equals("tEXt")) {
				return Problem.CORRUPT;
			} else if (name.equals("IDAT")) {
				pixels = true;
			} else if (name.equals("IEND")) {
				if (length != 0 || pos + 12 != data.length) {
					return Problem.CORRUPT;
				}
				ended = true;
				break;
			}
			pos += 12 + length;
		}
		if (!header || !pixels || !ended) {
			return Problem.CORRUPT;
		}
		try {
			javax.imageio.ImageIO.setUseCache(false);
			java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new ByteArrayInputStream(data));
			if (image == null || image.getWidth() != width || image.getHeight() != height) {
				return Problem.CORRUPT;
			}
		} catch (java.io.IOException | RuntimeException exception) {
			return Problem.CORRUPT;
		} catch (LinkageError error) {
			return Problem.CORRUPT;
		}
		return Problem.NONE;
	}

	public static String hash(byte[] data) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				builder.append(String.format("%02x", digest[i]));
			}
			return builder.toString();
		} catch (NoSuchAlgorithmException exception) {
			return Integer.toHexString(java.util.Arrays.hashCode(data));
		}
	}
}
