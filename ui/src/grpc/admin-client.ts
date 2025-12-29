import {createChannel, createClientFactory, Metadata} from 'nice-grpc-web'
import {AdminServiceDefinition} from '../gen/admin-service'
import {PROXY_URL} from "../config/config.ts";

console.log("Connecting to proxy: ", PROXY_URL);

export const getAdminPassword = () => {
    const adminPassword = localStorage.getItem('auth_token') || "";
    console.log("Using password: ", !!adminPassword);
    return adminPassword;
};

const channel = createChannel(PROXY_URL)

const clientFactory = createClientFactory()
    .use((call, options) => {
        const authMetadata = Metadata({ 'authorization': 'Bearer ' + getAdminPassword() });

        return call.next(call.request, {
            ...options,
            metadata: { ...options.metadata, ...authMetadata }
        });
    });

export const adminClient = clientFactory.create(AdminServiceDefinition, channel)