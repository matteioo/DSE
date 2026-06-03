<script setup lang="ts">
import type { VehicleState } from '@/types/simulation'

defineProps<{ vehicle: VehicleState }>()

function stateLabel(v: VehicleState): string {
  if (v.emergencyBrakeActive) return 'EMERGENCY BRAKE'
  return 'NORMAL'
}

function stateMod(v: VehicleState): string {
  if (v.emergencyBrakeActive) return 'emergency'
  return 'normal'
}

function fmtDist(d: number | null): string {
  return d === null ? '—' : d.toFixed(1) + ' m'
}
</script>

<template>
  <div class="card">
    <div class="card-header">
      <div class="title-row">
        <span class="vin">{{ vehicle.vin }}</span>
        <span class="role">{{ vehicle.role }}</span>
      </div>
      <span class="state-badge" :data-state="stateMod(vehicle)">{{ stateLabel(vehicle) }}</span>
    </div>

    <div class="card-body">
      <div class="row">
        <span class="lbl">GPS Latitude</span>
        <span class="val mono">{{ vehicle.latitude.toFixed(6) }}</span>
      </div>
      <div class="row">
        <span class="lbl">GPS Longitude</span>
        <span class="val mono">{{ vehicle.longitude.toFixed(6) }}</span>
      </div>
      <div class="row">
        <span class="lbl">Speed</span>
        <span class="val">{{ vehicle.speedKmh.toFixed(1) }} km/h</span>
      </div>
      <div class="row">
        <span class="lbl">Distance to Front</span>
        <span class="val">{{ fmtDist(vehicle.distanceToFrontM) }}</span>
      </div>
      <div class="row">
        <span class="lbl">Distance to Rear</span>
        <span class="val">{{ fmtDist(vehicle.distanceToRearM) }}</span>
      </div>
      <div class="row">
        <span class="lbl">Distance Change / s</span>
        <span class="val">
          {{ vehicle.distanceChangeMps > 0 ? '+' : '' }}{{ vehicle.distanceChangeMps.toFixed(2) }} m/s
        </span>
      </div>
      <div class="row">
        <span class="lbl">Emergency Brake Active</span>
        <span class="val">{{ vehicle.emergencyBrakeActive ? 'TRUE' : 'false' }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-background-soft);
}

.card-header {
  padding: 0.55rem 0.9rem;
  background: var(--color-background-mute);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 0.5rem;
}

.title-row {
  display: flex;
  align-items: baseline;
  gap: 0.5rem;
}

.vin {
  font-weight: 700;
  font-size: 1rem;
}

.role {
  font-size: 0.72rem;
  color: var(--color-text);
  opacity: 0.55;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.state-badge {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.18rem 0.45rem;
  border-radius: 4px;
  letter-spacing: 0.05em;
  white-space: nowrap;
}

.state-badge[data-state='normal'] {
  background: #14532d;
  color: #4ade80;
}
.state-badge[data-state='emergency'] {
  background: #7f1d1d;
  color: #fca5a5;
}

.card-body {
  padding: 0.5rem 0.9rem 0;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0.3rem 0;
  border-bottom: 1px solid var(--color-border);
  font-size: 0.86rem;
}

.row:last-child {
  border-bottom: none;
  padding-bottom: 0.5rem;
}

.lbl {
  opacity: 0.6;
}

.val {
  font-weight: 500;
}

.mono {
  font-family: monospace;
}
</style>
