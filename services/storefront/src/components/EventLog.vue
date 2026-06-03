<script setup lang="ts">
import type { EventLogEntry } from '@/types/simulation'

defineProps<{ entries: EventLogEntry[] }>()

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
  <div class="event-log">
    <h2>Event Log</h2>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Time</th>
            <th>Level</th>
            <th>Source</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(entry, i) in entries" :key="i" :data-level="levelMod(entry.level)">
            <td class="col-time">{{ fmtTime(entry.timestamp) }}</td>
            <td class="col-level">
              <span class="badge" :data-level="levelMod(entry.level)">{{ entry.level }}</span>
            </td>
            <td class="col-src">{{ entry.source }}</td>
            <td>{{ entry.message }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.event-log h2 {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 0.5rem;
  color: var(--color-heading);
}

.table-wrap {
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  max-height: 300px;
  overflow-y: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}

thead {
  position: sticky;
  top: 0;
  background: var(--color-background-mute);
  z-index: 1;
}

th {
  padding: 0.45rem 0.8rem;
  text-align: left;
  font-weight: 600;
  color: var(--color-heading);
  border-bottom: 1px solid var(--color-border);
}

td {
  padding: 0.35rem 0.8rem;
  border-bottom: 1px solid var(--color-border);
  vertical-align: top;
}

tr:last-child td {
  border-bottom: none;
}

tr[data-level='error'] td {
  background: rgba(239, 68, 68, 0.06);
}
tr[data-level='warn'] td {
  background: rgba(245, 158, 11, 0.05);
}

.col-time {
  white-space: nowrap;
  color: #64748b;
  font-family: monospace;
  font-size: 0.8rem;
}

.col-level {
  white-space: nowrap;
}

.col-src {
  white-space: nowrap;
  font-weight: 500;
}

.badge {
  font-size: 0.7rem;
  font-weight: 700;
  padding: 0.15rem 0.4rem;
  border-radius: 3px;
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
