<script setup lang="ts">
import type { VehicleState } from '@/types/simulation'
import { computed } from 'vue'

const props = defineProps<{ vehicles: VehicleState[] }>()

const lead = computed(() => props.vehicles.find((v) => v.role === 'LEAD'))
const follower = computed(() => props.vehicles.find((v) => v.role === 'FOLLOWER'))
const distance = computed(() => follower.value?.distanceToFrontM ?? 60)

const W = 700
const H = 150
const ROAD_Y = 75
const ROAD_H = 44
const CAR_W = 64
const CAR_H = 30

// Map distance 10 100 m pixel gap 44x400 px
const pixelGap = computed(() => Math.max(44, Math.min(400, distance.value * 3.8)))

const followerX = computed(() => 32)
const leadX = computed(() => followerX.value + pixelGap.value + CAR_W)

function carColor(v: VehicleState | undefined): string {
  if (!v) return '#4ade80'
  if (v.emergencyBrakeActive) return '#ef4444'
  if (v.preEmergencyBrake) return '#facc15'
  return '#4ade80'
}

function labelY(offset: number) {
  return ROAD_Y + ROAD_H / 2 + offset
}
</script>

<template>
  <div class="road-wrap">
    <svg :width="W" :height="H" :viewBox="`0 0 ${W} ${H}`" class="road-svg">
      <rect x="0" :y="ROAD_Y - ROAD_H / 2" :width="W" :height="ROAD_H" fill="#374151" rx="4" />

      <line
        x1="0"
        :y1="ROAD_Y"
        :x2="W"
        :y2="ROAD_Y"
        stroke="#facc15"
        stroke-width="2"
        stroke-dasharray="20,15"
      />

      <line
        :x1="followerX + CAR_W + 6"
        :y1="ROAD_Y - ROAD_H / 2 - 10"
        :x2="leadX - 6"
        :y2="ROAD_Y - ROAD_H / 2 - 10"
        stroke="#64748b"
        stroke-width="1.5"
      />
      <text
        :x="followerX + CAR_W + pixelGap / 2"
        :y="ROAD_Y - ROAD_H / 2 - 16"
        text-anchor="middle"
        font-size="13"
        fill="#94a3b8"
      >
        {{ distance.toFixed(1) }} m
      </text>

      <g :transform="`translate(${followerX}, ${ROAD_Y - CAR_H / 2})`">
        <rect :width="CAR_W" :height="CAR_H" :fill="carColor(follower)" rx="5" />
        <text x="32" y="19" text-anchor="middle" font-size="9" fill="#1e293b" font-weight="bold">
          {{ follower?.vin ?? 'VH-002' }}
        </text>
      </g>

      <g :transform="`translate(${leadX}, ${ROAD_Y - CAR_H / 2})`">
        <rect :width="CAR_W" :height="CAR_H" :fill="carColor(lead)" rx="5" />
        <text x="32" y="19" text-anchor="middle" font-size="9" fill="#1e293b" font-weight="bold">
          {{ lead?.vin ?? 'VH-001' }}
        </text>
      </g>

      <text
        :x="followerX + CAR_W / 2"
        :y="labelY(16)"
        text-anchor="middle"
        font-size="12"
        fill="#94a3b8"
      >
        ↑ {{ follower?.speedKmh.toFixed(0) }} km/h
      </text>
      <text
        :x="leadX + CAR_W / 2"
        :y="labelY(16)"
        text-anchor="middle"
        font-size="12"
        fill="#94a3b8"
      >
        ↑ {{ lead?.speedKmh.toFixed(0) }} km/h
      </text>

      <text :x="W - 28" :y="ROAD_Y + 5" font-size="18" fill="#4b5563">→</text>
    </svg>

    <div class="legend">
      <span class="dot normal"></span>Normal
      <span class="dot pre-emergency"></span>Pre-Emergency
      <span class="dot emergency"></span>Emergency Brake
    </div>
  </div>
</template>

<style scoped>
.road-wrap {
  background: #1e293b;
  border-radius: 10px;
  padding: 1rem 1.2rem 0.8rem;
  overflow-x: auto;
}

.road-svg {
  display: block;
  max-width: 100%;
}

.legend {
  display: flex;
  gap: 1.4rem;
  align-items: center;
  font-size: 0.8rem;
  color: #94a3b8;
  margin-top: 0.6rem;
  padding-top: 0.6rem;
  border-top: 1px solid #334155;
}

.dot {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 2px;
  margin-right: 5px;
  vertical-align: middle;
}

.dot.normal {
  background: #4ade80;
}
.dot.pre-emergency {
  background: #facc15;
}
.dot.emergency {
  background: #ef4444;
}
</style>
