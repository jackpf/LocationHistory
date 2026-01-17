import {ClientError, createChannel, createClientFactory, Metadata, Status} from "nice-grpc-web"
import {AdminServiceDefinition} from "../gen/admin-service"
import {PROXY_URL} from "../config/config.ts";
import {getTokenFromStorage} from "../hooks/use-login.ts";

console.log("Connecting to proxy: ", PROXY_URL);

const channel = createChannel(PROXY_URL)

export const AUTH_ERROR_EVENT = "auth-error";
export type AuthErrorEvent = CustomEvent<{ message: string }>;

const clientFactory = createClientFactory()
    .use((call, options) => {
        const authMetadata = Metadata({ "authorization": "Bearer " + getTokenFromStorage() });

        const response = call.next(call.request, {
            ...options,
            metadata: { ...options.metadata, ...authMetadata }
        });

        // Wrap the response to intercept authentication errors
        return (async function* () {
            try {
                return yield* response;
            } catch (error) {
                if (error instanceof ClientError && error.code === Status.UNAUTHENTICATED) {
                    window.dispatchEvent(new CustomEvent(AUTH_ERROR_EVENT, {
                        detail: { message: "Unauthenticated. Please log in again." }
                    }));
                }
                throw error;
            }
        })();
    });

export const adminClient = clientFactory.create(AdminServiceDefinition, channel)

export const grpcErrorMessage = (message: string, error: any) => {
    if (error instanceof ClientError) return message + ": " + error.details;
    else return message + ": " + error.message;
}