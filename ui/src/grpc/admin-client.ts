import {createChannel, createClientFactory, Metadata} from 'nice-grpc-web'
import {AdminServiceDefinition} from '../gen/admin-service'

const proxyUrl = import.meta.env.VITE_PROXY_URL
const channel = createChannel(proxyUrl)

const clientFactory = createClientFactory()
    .use((call, options) => {
        const authMetadata = Metadata({ 'authorization': 'Bearer admin' });

        return call.next(call.request, {
            ...options,
            metadata: { ...options.metadata, ...authMetadata }
        });
    });

export const adminClient = clientFactory.create(AdminServiceDefinition, channel)