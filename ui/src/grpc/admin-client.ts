import {createChannel, createClientFactory, Metadata} from 'nice-grpc-web'
import {AdminServiceDefinition} from '../gen/admin-service'

const proxyUrl = import.meta.env.VITE_PROXY_URL
const adminPassword = import.meta.env.VITE_ADMIN_PASSWORD
const channel = createChannel(proxyUrl)

const clientFactory = createClientFactory()
    .use((call, options) => {
        const authMetadata = Metadata({ 'authorization': 'Bearer ' + adminPassword });

        return call.next(call.request, {
            ...options,
            metadata: { ...options.metadata, ...authMetadata }
        });
    });

export const adminClient = clientFactory.create(AdminServiceDefinition, channel)