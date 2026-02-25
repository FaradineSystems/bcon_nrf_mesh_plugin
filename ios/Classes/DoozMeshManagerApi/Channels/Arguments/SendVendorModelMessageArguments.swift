//
//  SendVendorModelMessageArguments.swift
//  nordic_nrf_mesh
//
//  Created by Joseph Lindemuth on 19 Feb 2026.
//

struct SendVendorModelMessageArguments: BaseFlutterArguments {
    let address: Int
    let modelId: UInt32
    let keyIndex: Int
    let companyIdentifier: UInt32
    let opCode: UInt32
    let parameters: [UInt8]
}
