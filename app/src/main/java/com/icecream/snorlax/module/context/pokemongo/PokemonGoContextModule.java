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

package com.icecream.snorlax.module.context.pokemongo;

import javax.inject.Singleton;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;

import dagger.Module;
import dagger.Provides;

@Module
@SuppressWarnings({"unused", "FieldCanBeLocal", "WeakerAccess"})
public final class PokemonGoContextModule {

	@Provides
	@PokemonGo
	@Singleton
	static Context provideContext(Application application) {
		try {
			return PokemonGoContextFactory.create(application);
		}
		catch (PackageManager.NameNotFoundException e) {
			throw new RuntimeException("Pokemon Go package not found... Cannot continue");
		}
	}

	@Provides
	@PokemonGo
	@Singleton
	static NotificationManager provideNotificationManager(@PokemonGo Context context) {
		return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
	}
}
