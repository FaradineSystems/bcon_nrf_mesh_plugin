//
//  SendVendorModelMessageArguments.swift
//  nordic_nrf_mesh
//
//  Created by Joseph Lindemuth on 19 Feb 2026.
//

struct SendVendorModelMessageArguments: BaseFlutterArguments {
    let address: Int
    let modelId: Int
    let keyIndex: Int
    let companyIdentifier: Int
    let opCode: UInt32
    let parameters: [UInt8]
}
