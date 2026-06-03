<script setup lang="ts">
import type { VehicleState, EventLogEntry } from '@/types/simulation'

defineProps<{
  vehicles: VehicleState[]
  eventLog: EventLogEntry[]
}>()

function fmtDist(d: number | null): string {
  return d !== null ? d.toFixed(1) + ' m' : '—'
}

function fmtRate(r: number): string {
  return (r > 0 ? '+' : '') + r.toFixed(2) + ' m/s'
}

function fmtTime(ts: string): string {
  return new Date(ts).toLocaleTimeString()
}

function levelMod(level: string): string {
  if (level === 'WARN') return 'warn'
  if (level === 'ERROR') return 'error'
  return 'info'
}
</script>

<template>
  <div class="backend-panel">

    <div class="sub-section">
      <h3>Position &amp; Distance Overview</h3>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Vehicle</th>
              <th>GPS Latitude</th>
              <th>GPS Longitude</th>
              <th>Dist. to Front</th>
              <th>Dist. to Rear</th>
              <th>D / s</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="v in vehicles" :key="v.vin">
              <td class="col-vin">
                <strong>{{ v.vin }}</strong>
                <span class="role-tag">{{ v.role }}</span>
              </td>
              <td class="mono">{{ v.latitude.toFixed(6) }}</td>
              <td class="mono">{{ v.longitude.toFixed(6) }}</td>
              <td>{{ fmtDist(v.distanceToFrontM) }}</td>
              <td>{{ fmtDist(v.distanceToRearM) }}</td>
              <td >
                {{ fmtRate(v.distanceChangeMps) }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="sub-section">
      <h3>ORCHESTRATOR Event Log</h3>
      <div class="log-wrap">
        <table class="log-table">
          <thead>
            <tr>
              <th>Time</th>
              <th>Level</th>
              <th>Source</th>
              <th>Message</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(e, i) in eventLog" :key="i" :data-level="levelMod(e.level)">
              <td class="col-time">{{ fmtTime(e.timestamp) }}</td>
              <td>
                <span class="badge" :data-level="levelMod(e.level)">{{ e.level }}</span>
              </td>
              <td class="col-src">{{ e.source }}</td>
              <td>{{ e.message }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

  </div>
</template>

<style scoped>
.backend-panel {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  background: var(--color-background-soft);
}

.sub-section {
  padding: 0.8rem 1rem;
  border-bottom: 1px solid var(--color-border);
}

.sub-section:last-child {
  border-bottom: none;
}

h3 {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--color-heading);
  opacity: 0.6;
  margin-bottom: 0.55rem;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.86rem;
}

thead {
  background: var(--color-background-mute);
}

th {
  padding: 0.4rem 0.7rem;
  text-align: left;
  font-weight: 600;
  color: var(--color-heading);
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

td {
  padding: 0.4rem 0.7rem;
  border-bottom: 1px solid var(--color-border);
}

tr:last-child td {
  border-bottom: none;
}

.col-vin {
  display: flex;
  align-items: center;
  gap: 0.4rem;
}

.role-tag {
  font-size: 0.68rem;
  opacity: 0.5;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.mono {
  font-family: monospace;
  font-size: 0.82rem;
}

.log-wrap {
  max-height: 220px;
  overflow-y: auto;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  overflow-x: hidden;
}

.log-table {
  font-size: 0.84rem;
}

.log-table thead {
  position: sticky;
  top: 0;
  z-index: 1;
}

tr[data-level='error'] td {
  background: rgba(239, 68, 68, 0.07);
}
tr[data-level='warn'] td {
  background: rgba(245, 158, 11, 0.06);
}

.col-time {
  white-space: nowrap;
  color: #64748b;
  font-family: monospace;
  font-size: 0.8rem;
}

.col-src {
  white-space: nowrap;
  font-weight: 600;
}

.badge {
  font-size: 0.68rem;
  font-weight: 700;
  padding: 0.12rem 0.38rem;
  border-radius: 3px;
  white-space: nowrap;
}

.badge[data-level='info'] {
  background: #1e3a5f;
  color: #93c5fd;
}
.badge[data-level='warn'] {
  background: #78350f;
  color: #fcd34d;
}
.badge[data-level='error'] {
  background: #7f1d1d;
  color: #fca5a5;
}
</style>
