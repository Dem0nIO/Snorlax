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

package com.icecream.snorlax.module.feature.broadcast;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.util.Pair;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.icecream.snorlax.module.feature.Feature;
import com.icecream.snorlax.module.feature.mitm.MitmRelay;
import com.icecream.snorlax.module.util.Log;
import com.icecream.snorlax.module.util.RxFuncitons;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import POGOProtos.Map.Pokemon.NearbyPokemonOuterClass;
import POGOProtos.Map.Pokemon.WildPokemonOuterClass;
import POGOProtos.Networking.Responses.GetMapObjectsResponseOuterClass;
import POGOProtos.Map.MapCellOuterClass.MapCell;
import rx.Observable;
import rx.Subscription;

import static POGOProtos.Networking.Requests.RequestOuterClass.Request;
import static POGOProtos.Networking.Requests.RequestTypeOuterClass.RequestType;

@Singleton
public final class Broadcast implements Feature {

    private final MitmRelay mMitmRelay;
    private final BroadcastPreferences mPreferences;
    private final BroadcastNotification mBroadcastNotification;
    private Subscription mSubscription;

    @Inject
    Broadcast(MitmRelay mitmRelay, BroadcastPreferences preferences, BroadcastNotification notification) {
        mMitmRelay = mitmRelay;
        mPreferences = preferences;
        mBroadcastNotification = notification;
    }

    @Override
    public void subscribe() {
        unsubscribe();

        mSubscription = mMitmRelay
            .getObservable()
            .compose(mPreferences.isEnabled())
            .flatMap(envelope -> {
                List<Request> requests = envelope.getRequest().getRequestsList();

                for (int i = 0; i < requests.size(); i++) {
                    RequestType type = requests.get(i).getRequestType();

                    switch (type) {
                        case GET_MAP_OBJECTS:
                            return Observable.just(new Pair<>(type, envelope.getResponse().getReturns(i)));
                        default:
                            break;
                    }
                }
                return Observable.empty();
            })
            .subscribe(pair -> {
                switch (pair.first) {
                    case GET_MAP_OBJECTS:
                        onGetMapObjects(pair.second);
                        break;
                    default:
                        break;
                }
            }, Log::e);
    }

    private void onGetMapObjects(ByteString bytes) {
        try {
            GetMapObjectsResponseOuterClass.GetMapObjectsResponse response = GetMapObjectsResponseOuterClass.GetMapObjectsResponse.parseFrom(bytes);
            mBroadcastNotification.show("onGetMapObjects");

            List<MapCell> mapCellsList = response.getMapCellsList();
            JSONObject jsonArrayData = new JSONObject();
            int mapCellsCount = response.getMapCellsCount();
            for(int i = 0; i < mapCellsCount; i++) {
                MapCell cell = mapCellsList.get(i);

                int wildPokemonCount = cell.getWildPokemonsCount();
                JSONArray jsonArrayWildPokemon = new JSONArray();
                for (int j = 0; j < wildPokemonCount; j++) {
                    WildPokemonOuterClass.WildPokemon pokemon = cell.getWildPokemons(j);
                    JSONObject jsonPokemon = new JSONObject();
                    jsonPokemon.put("id", pokemon.getEncounterId());
                    jsonPokemon.put("pokedex", pokemon.getPokemonData().getPokemonIdValue());
                    jsonPokemon.put("timeTillHiddenMs", pokemon.getTimeTillHiddenMs());
                    jsonPokemon.put("lastModifiedTimestampMs", pokemon.getLastModifiedTimestampMs());
                    jsonArrayWildPokemon.put(jsonPokemon);
                }
                jsonArrayData.put("wild", jsonArrayWildPokemon);

                int nearbyPokemonCount = cell.getNearbyPokemonsCount();
                JSONArray jsonArrayNearbyPokemon = new JSONArray();
                for (int j = 0; j < nearbyPokemonCount; j++) {
                    NearbyPokemonOuterClass.NearbyPokemon pokemon = cell.getNearbyPokemons(j);
                    JSONObject jsonPokemon = new JSONObject();
                    jsonPokemon.put("id", pokemon.getEncounterId());
                    jsonPokemon.put("pokedex", pokemon.getPokemonIdValue());
                    jsonArrayNearbyPokemon.put(jsonPokemon);
                }
                jsonArrayData.put("nearby", jsonArrayNearbyPokemon);
            }

            Intent getMapObjectsIntent = new Intent()
                    .setAction("com.icecream.snorlax.BROADCAST_GETMAPOBJECTS")
                    .setType("application/json")
                    .putExtra("data", jsonArrayData.toString());
            mBroadcastNotification.send(getMapObjectsIntent);
        }
        catch (JSONException e){
            Log.d("GetMapObjectsResponse json exception: %s" + e.getMessage());
            Log.e(e);
        }
        catch (InvalidProtocolBufferException | NullPointerException e) {
            Log.d("GetMapObjectsResponse failed: %s" + e.getMessage());
            Log.e(e);
        }
    }

    @Override
    public void unsubscribe() {
        RxFuncitons.unsubscribe(mSubscription);
    }
}
