/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.lost;

import org.terasology.assets.management.AssetManager;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.players.event.OnPlayerSpawnedEvent;
import org.terasology.lost.generator.LostWorldGenerator;
import org.terasology.math.Region3i;
import org.terasology.registry.In;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.WorldProvider;
import org.terasology.world.generation.facets.SurfaceHeightFacet;

import static org.terasology.lost.LevelSpawnSystem.spawnLevel;

/**
 * Contains actions to be taken when the player spawns in the Lost world for the first time
 */
@RegisterSystem(RegisterMode.AUTHORITY)
public class OnSpawnSystem extends BaseComponentSystem {
    @In
    private EntityManager entityManager;
    @In
    private InventoryManager inventoryManager;
    @In
    private AssetManager assetManager;
    @In
    private WorldProvider worldProvider;

    private static final int HUT_OFFSET_FROM_SPAWN = 15;

    @ReceiveEvent
    public void onPlayerSpawn(OnPlayerSpawnedEvent event, EntityRef player, InventoryComponent inventory) {
        inventoryManager.giveItem(player, null, entityManager.create("Lost:antrumSabre"));
        ProgressTrackingComponent progressTrackingComponent = new ProgressTrackingComponent();
        // set level prefabs according to the biome they are supposed to spawn in
        progressTrackingComponent.addLevel("Lost:well", "Beach", "Coast", "Lakeshore");
        progressTrackingComponent.addLevel("Lost:templePlasmaOfFire", "Tropical seasonal forest", "Tropical rain " +
                "forest", "Temperate deciduous forest", "Temperate rain forest");
        progressTrackingComponent.addLevel("Lost:stonehengeWipedOutKey", "Marsh", "Shrubland", "Grassland");
        progressTrackingComponent.addLevel("Lost:pyramidBladesOfTruth", "Bare", "Scorched", "Temperate " +
                "desert", "Subtropical desert");
        player.addComponent(progressTrackingComponent);

        LocationComponent loc = player.getComponent(LocationComponent.class);
        Vector3f playerLocation = loc.getWorldPosition();
        // radius such that hut is in the search area
        int searchRadius = 25;

        // create Region to be searched
        Vector3i extent = new Vector3i(searchRadius, 1, searchRadius);
        Vector3i desiredPos = new Vector3i(playerLocation.getX(), 1, playerLocation.getZ());
        Region3i searchRegion = Region3i.createFromCenterExtents(desiredPos, extent);

        // fetch surface height facet
        org.terasology.world.generation.Region worldRegion = LostWorldGenerator.world.getWorldData(searchRegion);
        SurfaceHeightFacet surfaceHeightFacet = worldRegion.getFacet(SurfaceHeightFacet.class);

        // spawn the hut a little far from the player
        int x = (int) Math.round(playerLocation.getX()) - HUT_OFFSET_FROM_SPAWN;
        int y = (int) Math.round(playerLocation.getZ()) - HUT_OFFSET_FROM_SPAWN;
        Vector3i spawnPosition = new Vector3i(x, surfaceHeightFacet.getWorld(x, y), y);
        spawnLevel("Lost:hut", spawnPosition, assetManager, entityManager);
        spawnPosition.y = 0;
        progressTrackingComponent.hutPosition = spawnPosition;

    }
}
