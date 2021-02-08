/*
 * Copyright (C) 2015-2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.content.pm

import android.os.*

interface IPackageInstallObserver : IInterface {

    @Throws(RemoteException::class)
    fun packageInstalled(packageName: String?, returnCode: Int)

    abstract class Stub : Binder(), IPackageInstallObserver {
        override fun asBinder(): IBinder {
            throw RuntimeException("Stub!")
        }

        @Throws(RemoteException::class)
        public override fun onTransact(
            code: Int,
            data: Parcel,
            reply: Parcel?,
            flags: Int
        ): Boolean {
            throw RuntimeException("Stub!")
        }

        companion object {
            fun asInterface(obj: IBinder?): IPackageInstallObserver {
                throw RuntimeException("Stub!")
            }
        }

        init {
            throw RuntimeException("Stub!")
        }
    }
}