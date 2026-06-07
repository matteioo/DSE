import { ref } from 'vue'
import { Client } from '@stomp/stompjs'

export type ScenarioCommand = 'SCENARIO_1' | 'SCENARIO_2' | 'SCENARIO_3' | 'RESET' | 'IDLE'

export const stompConnected = ref(false)

const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
const brokerURL = `${proto}//${window.location.host}/stomp`

const client = new Client({
  brokerURL,
  connectHeaders: { login: 'dse', passcode: 'dse' },
  reconnectDelay: 5000,
  onConnect:    () => { stompConnected.value = true },
  onDisconnect: () => { stompConnected.value = false },
  onStompError: () => { stompConnected.value = false },
})

client.activate()

export function publishScenario(scenario: ScenarioCommand): void {
  if (!client.connected) {
    throw new Error('Not connected to RabbitMQ — is the broker running?')
  }
  client.publish({
    destination: '/exchange/d2s.simulator.scenario/scenario',
    body: scenario,
  })
}
