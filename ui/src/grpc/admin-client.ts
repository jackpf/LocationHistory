import {createChannel, createClientFactory, Metadata} from 'nice-grpc-web'
import {AdminServiceDefinition} from '../gen/admin-service'
import {ADMIN_PASSWORD, PROXY_URL} from "../config/config.ts";

console.log("Connecting to proxy: ", PROXY_URL);
console.log("Using password: ", !!ADMIN_PASSWORD);

const channel = createChannel(PROXY_URL)

const clientFactory = createClientFactory()
    .use((call, options) => {
        const authMetadata = Metadata({ 'authorization': 'Bearer ' + ADMIN_PASSWORD });

        return call.next(call.request, {
            ...options,
            metadata: { ...options.metadata, ...authMetadata }
        });
    });

export const adminClient = clientFactory.create(AdminServiceDefinition, channel)