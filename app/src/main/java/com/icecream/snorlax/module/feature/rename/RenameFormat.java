/*
 * Copyright (c) 2016. Pedro Diaz <igoticecream@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icecream.snorlax.module.feature.rename;

import javax.inject.Inject;
import javax.inject.Singleton;

import android.support.annotation.Nullable;

import com.icecream.snorlax.common.Decimals;
import com.icecream.snorlax.common.Strings;
import com.icecream.snorlax.module.Pokemons;

import static POGOProtos.Data.PokemonDataOuterClass.PokemonData;
import static android.R.attr.level;
import static java.lang.Integer.parseInt;

@Singleton
final class RenameFormat {

	private static final String BASE_NICK = "NICK";
	private static final String BASE_LVL = "LVL";
	private static final String BASE_IV = "IV";

	private final Pokemons mPokemons;
	private final RenamePreferences mRenamePreferences;

	@Inject
	RenameFormat(Pokemons pokemons, RenamePreferences renamePreferences) {
		mPokemons = pokemons;
		mRenamePreferences = renamePreferences;
	}

	@Nullable
	private String processNick(String target, String nick) {
		final int length = target.length();
		final int dot = target.indexOf('.') + 1;

		if (length == BASE_NICK.length()) {
			return nick;
		}
		else if (dot > 0 && length > dot) {
			try {
				return Strings.truncateAt(nick, parseInt(target.substring(dot)));
			}
			catch (NumberFormatException ignored) {
			}
		}
		return null;
	}

	@Nullable
	private String processLevel(String target, float level) {

		if (target.equals(BASE_LVL)) {
			return Decimals.format(level, 1, 1);
		}
		if (target.equals(BASE_LVL.concat("P"))) {
			return Decimals.format(level, 2, 1);
		}
		if (target.startsWith(BASE_LVL.concat("."))) {
			try {
				return Decimals.format(level, 1, Integer.parseInt(target.substring(target.indexOf('.') + 1)));
			}
			catch (NumberFormatException | IndexOutOfBoundsException ignored) {
			}
		}
		if (target.startsWith(BASE_LVL.concat("P."))) {
			try {
				return Decimals.format(level, 2, Integer.parseInt(target.substring(target.indexOf('.') + 1)));
			}
			catch (NumberFormatException | IndexOutOfBoundsException ignored) {
			}
		}
		return null;
	}

	@Nullable
	private String processIv(String target, double iv) {
		if (target.equals(BASE_IV)) {
			return Decimals.format(iv, 1, 1);
		}
		if (target.equals(BASE_IV.concat("P"))) {
			return Decimals.format(iv, 3, 1);
		}
		if (target.startsWith(BASE_IV.concat("."))) {
			try {
				return Decimals.format(iv, 1, Integer.parseInt(target.substring(target.indexOf('.') + 1)));
			}
			catch (NumberFormatException | IndexOutOfBoundsException ignored) {
			}
		}
		if (target.startsWith(BASE_IV.concat("P."))) {
			try {
				return Decimals.format(iv, 3, Integer.parseInt(target.substring(target.indexOf('.') + 1)));
			}
			catch (NumberFormatException | IndexOutOfBoundsException ignored) {
			}
		}
		return null;
	}

	private String processFormat(Pokemons.Data pokemonsData, String command) throws NullPointerException {
		final String target = command.toUpperCase();

		String processed = null;

		if (target.startsWith(BASE_NICK)) {
			processed = processNick(target, pokemonsData.getName());
		}
		else if (target.startsWith(BASE_LVL)) {
			processed = processLevel(target, pokemonsData.getLevel());
		}
		else if (target.startsWith(BASE_IV)) {
			processed = processIv(target, pokemonsData.getIvRatio() * 100);
		}

		return Strings.isNullOrEmpty(processed) ? "%" + command + "%" : processed;
	}

	String format(PokemonData pokemonData) throws NullPointerException, IllegalArgumentException {
		final Pokemons.Data data = mPokemons.with(pokemonData);
		final String format = mRenamePreferences.getFormat();

		StringBuilder builder = new StringBuilder();

		for (int i = 0, len = format.length(); i < len; ) {
			int nextPercent = format.indexOf('%', i + 1);
			if (format.charAt(i) != '%') {
				final int end = (nextPercent == -1) ? len : nextPercent;

				builder.append(format.substring(i, end));
				i = end;
			}
			else if (nextPercent == -1) {
				builder.append(format.substring(i));
				i = len;
			}
			else if (format.substring(i + 1, nextPercent).contains(" ")) {
				builder.append(format.substring(i, nextPercent));
				i = nextPercent;
			}
			else {
				builder.append(processFormat(data, format.substring(i + 1, nextPercent)));
				i = nextPercent + 1;
			}
		}

		return builder.toString();
	}
}
