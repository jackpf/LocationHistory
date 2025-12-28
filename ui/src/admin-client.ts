import { createClient, createChannel } from 'nice-grpc-web'
import { AdminServiceDefinition } from './gen/admin-service'

const channel = createChannel('http://localhost:9123')
export const adminClient = createClient(AdminServiceClient, channel)