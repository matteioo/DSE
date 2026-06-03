<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useSimulationStore } from '@/stores/simulation'
import RoadView from '@/components/RoadView.vue'
import VehicleCard from '@/components/VehicleCard.vue'
import BackendPanel from '@/components/BackendPanel.vue'

const store = useSimulationStore()

onMounted(() => store.startPolling())
onUnmounted(() => store.stopPolling())
</script>

<template>
  <div class="sim">

    <header class="sim-header">
      <h1> Simulation Monitor</h1>
      <span v-if="store.error" class="badge error">{{ store.error }}</span>
      <span v-else class="badge live">LIVE</span>
    </header>

    <template v-if="store.vehicles.length > 0">

      <section class="section">
        <h2 class="section-title">Road</h2>
        <RoadView :vehicles="store.vehicles" />
      </section>

      <section class="section">
        <h2 class="section-title">Vehicle Data</h2>
        <div class="vehicles-grid">
          <VehicleCard
            v-for="v in store.vehicles"
            :key="v.vin"
            :vehicle="store.vehicleDetails[v.vin] ?? v"
          />
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">Backend Microservices</h2>
        <BackendPanel
          :vehicles="store.vehicles"
          :event-log="store.events"
        />
      </section>

    </template>

    <div v-else-if="!store.loading" class="no-data">
      Unable to load simulation data. Is Spider running?
    </div>

  </div>
</template>

<style scoped>
.sim {
  max-width: 1100px;
  margin: 0 auto;
  padding: 1.2rem 1.5rem 2.5rem;
}

.sim-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.4rem;
  padding-bottom: 0.7rem;
  border-bottom: 2px solid var(--color-border);
}

.sim-header h1 {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--color-heading);
  flex: 1;
}

.badge {
  font-size: 0.72rem;
  font-weight: 700;
  padding: 0.2rem 0.55rem;
  border-radius: 4px;
}

.badge.live   { background: #14532d; color: #4ade80; }
.badge.muted  { background: var(--color-background-mute); color: var(--color-text); opacity: .65; }
.badge.error  { background: #7f1d1d; color: #fca5a5; }

.section {
  margin-bottom: 1.6rem;
}

.section-title {
  font-size: 0.72rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--color-heading);
  opacity: 0.45;
  margin-bottom: 0.55rem;
}

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(310px, 1fr));
  gap: 1rem;
}

.no-data {
  text-align: center;
  padding: 4rem;
  opacity: 0.4;
}
</style>
