<script setup lang="ts">
import { ref } from 'vue'
import { publishScenario, stompConnected, type ScenarioCommand } from '@/api/rabbitmqApi'

const lastSent = ref<string | null>(null)
const error    = ref<string | null>(null)

const scenarios: { cmd: ScenarioCommand; label: string; description: string }[] = [
  { cmd: 'SCENARIO_1', label: 'Scenario 1', description: 'Normal following distance, no braking' },
  { cmd: 'SCENARIO_2', label: 'Scenario 2', description: 'Emergency braking triggered' },
  { cmd: 'SCENARIO_3', label: 'Scenario 3', description: 'Vehicles approach pre-emergency threshold' },
]

function send(cmd: ScenarioCommand) {
  error.value = null
  try {
    if (cmd !== 'RESET') publishScenario('RESET')
    publishScenario(cmd)
    lastSent.value = cmd
  } catch (e: any) {
    error.value = e.message ?? 'Failed to publish scenario'
  }
}
</script>

<template>
  <div class="scenario-page">

    <header class="page-header">
      <h1>Scenario Control</h1>
      <span v-if="lastSent" class="badge sent">Sent: {{ lastSent }}</span>
      <span class="badge" :class="stompConnected ? 'connected' : 'disconnected'">
        {{ stompConnected ? 'RabbitMQ connected' : 'Connecting…' }}
      </span>
    </header>

    <p class="desc">
      Broadcasts a scenario command directly to the <code>d2s.simulator.scenario</code> fanout exchange.
      All simulation services (simulator, orchestrator, brakenow, utracked) receive it simultaneously.
    </p>

    <div class="scenario-grid">
      <button
        v-for="s in scenarios"
        :key="s.cmd"
        class="scenario-btn"
        :class="{ active: lastSent === s.cmd }"
        :disabled="!stompConnected"
        @click="send(s.cmd)"
      >
        <span class="btn-label">{{ s.label }}</span>
        <span class="btn-desc">{{ s.description }}</span>
      </button>
    </div>

    <div class="divider" />

   <button
     class="reset-btn"
     :disabled="!stompConnected"
     @click="send('RESET')"
   >
      Reset Simulation
    </button>

    <p v-if="error" class="error-msg">{{ error }}</p>

  </div>
</template>

<style scoped>
.scenario-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 1.4rem 1.5rem 3rem;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 0.9rem;
  padding-bottom: 0.7rem;
  border-bottom: 2px solid var(--color-border);
}

.page-header h1 {
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

.badge.sent       { background: #14532d; color: #4ade80; }
.badge.connected  { background: #14532d; color: #4ade80; }
.badge.disconnected { background: #1e293b; color: #64748b; }

.desc {
  font-size: 0.88rem;
  color: var(--color-text);
  opacity: 0.65;
  margin-bottom: 1.6rem;
  line-height: 1.5;
}

.desc code {
  font-family: monospace;
  font-size: 0.85em;
  background: var(--color-background-mute);
  padding: 0.1em 0.35em;
  border-radius: 3px;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.9rem;
  margin-bottom: 1.2rem;
}

.scenario-btn {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  padding: 1rem 1.1rem;
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.15s, background 0.15s;
  color: var(--color-text);
}

.scenario-btn:hover:not(:disabled) {
  border-color: #4ade80;
  background: var(--color-background-mute);
}

.scenario-btn.active {
  border-color: #4ade80;
  background: rgba(74, 222, 128, 0.08);
}

.scenario-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-label {
  font-weight: 700;
  font-size: 1rem;
  color: var(--color-heading);
}

.btn-desc {
  font-size: 0.8rem;
  opacity: 0.6;
  line-height: 1.4;
}

.divider {
  border-top: 1px solid var(--color-border);
  margin: 1.2rem 0;
}

.reset-btn {
  padding: 0.6rem 1.4rem;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
  border-radius: 6px;
  color: #fca5a5;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background 0.15s;
}

.reset-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.2);
}

.reset-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.error-msg {
  margin-top: 1rem;
  font-size: 0.85rem;
  color: #fca5a5;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid #ef4444;
  border-radius: 6px;
  padding: 0.5rem 0.9rem;
}
</style>
